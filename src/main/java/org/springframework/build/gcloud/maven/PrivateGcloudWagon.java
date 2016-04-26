 /*
 * Copyright 2010-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.build.gcloud.maven;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;

import com.google.cloud.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.Storage.BlobListOption;

/**
 * An implementation of the Maven Wagon interface that allows you to access the Google Cloud Storage service. URLs that reference
 * the Google Cloud Storage service should be in the form of <code>gcloud://bucket.name</code>. As an example
 * <code>gcloud://static.springframework.org</code> would put files into the <code>static.springframework.org</code> bucket
 * on the Google Cloud Storage service.
 * <p/>
 */
public final class PrivateGcloudWagon extends AbstractWagon {

    private static final String KEY_FORMAT = "%s%s";

    private static final String RESOURCE_FORMAT = "%s(.*)";

    private volatile Storage storage;

    private volatile String bucketName;

    private volatile String baseDirectory;

    /**
     * Creates a new instance of the wagon
     */
    public PrivateGcloudWagon() {
        super(false);
    }

    PrivateGcloudWagon(Storage storage, String bucketName, String baseDirectory) {
        super(false);
        this.storage = storage;
        this.bucketName = bucketName;
        this.baseDirectory = baseDirectory;
    }

    @Override
    protected void connectToRepository(Repository repository, AuthenticationInfo authenticationInfo, ProxyInfoProvider proxyInfoProvider) {
        if (this.storage == null) {
            this.bucketName = GcloudUtils.getBucketName(repository);
            this.baseDirectory = GcloudUtils.getBaseDirectory(repository);
            this.storage = StorageOptions.defaultInstance().service();
        }
    }

    @Override
    protected void disconnectFromRepository() {
        this.storage = null;
        this.bucketName = null;
        this.baseDirectory = null;
    }

    @Override
    protected boolean doesRemoteResourceExist(String resourceName) {
        if (this.storage.get(this.bucketName, resourceName) == null) {
          return false;
        }
        return true;
    }

    @Override
    protected boolean isRemoteResourceNewer(String resourceName, long timestamp) throws ResourceDoesNotExistException {
        Blob blob = this.storage.get(this.bucketName, resourceName);
        if (blob == null) {
            throw new ResourceDoesNotExistException(String.format("'%s' does not exist", resourceName));
        } 

        Long lastModified = this.storage.get(this.bucketName, resourceName).updateTime();
        return lastModified > timestamp;
    }

    @Override
    protected List<String> listDirectory(String directory) {
        List<String> directoryContents = new ArrayList<String>();

        String prefix = getKey(directory);
        BlobListOption options = Storage.BlobListOption.prefix(prefix);

        Page<Blob> blobs = this.storage.list(this.bucketName, options);
        Iterator<Blob> iterator = blobs.iterateAll();

        while (iterator.hasNext()) {
            Blob blob = iterator.next();
            directoryContents.add(blob.name()); 
        }

        return directoryContents;
    }

    @Override
    protected void getResource(String resourceName, File destination, TransferProgress transferProgress) throws TransferFailedException,
        ResourceDoesNotExistException {
        InputStream in = null;
        OutputStream out = null;
        try {
            Blob blob = this.storage.get(this.bucketName, getKey(resourceName));
            if (blob == null) {
                throw new ResourceDoesNotExistException(String.format("'%s' does not exist", resourceName));
            }

            in = Channels.newInputStream(blob.reader());
            out = new TransferProgressFileOutputStream(destination, transferProgress);

            IoUtils.copy(in, out);
        } catch (FileNotFoundException e) {
            throw new TransferFailedException(String.format("Cannot write file to '%s'", destination), e);
        } catch (IOException e) {
            throw new TransferFailedException(String.format("Cannot read from '%s' and write to '%s'", resourceName, destination), e);
        } finally {
            IoUtils.closeQuietly(in, out);
        }
    }

    @Override
    protected void putResource(File source, String destination, TransferProgress transferProgress) throws TransferFailedException,
        ResourceDoesNotExistException {
        InputStream in = null;
        OutputStream out = null;
        try {
            Blob blob = this.storage.create(BlobInfo.builder(this.bucketName, getKey(destination)).build());
            out =  Channels.newOutputStream(blob.writer());
            in = new TransferProgressFileInputStream(source, transferProgress);

            IoUtils.copy(in, out);
        } catch (StorageException e) {
            throw new TransferFailedException(String.format("Cannot write file to '%s'", destination), e);
        } catch (IOException e) {
            throw new TransferFailedException(String.format("Cannot read from '%s' and write to '%s'", destination, source), e);
        } finally {
            IoUtils.closeQuietly(in, out);
        }
    }

    private String getKey(String resourceName) {
        return String.format(KEY_FORMAT, this.baseDirectory, resourceName);
    }
}
