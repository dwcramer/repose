package com.rackspace.papi.components.datastore.distributed;

import com.rackspace.papi.components.datastore.Datastore;
import com.rackspace.papi.components.datastore.DatastoreOperationException;
import com.rackspace.papi.components.datastore.StoredElement;

import java.util.concurrent.TimeUnit;

/**
 * An interface for a Distributed Datastore that extends the behavior of a Datastore.
 * Distributed datastores allow retrieval and storage of data across local and remote datastores.
 */
public interface DistributedDatastore extends Datastore {

    /**
     * Put an element in a local or remote datastore for a duration of time not to exceed the TimeUnit and duration
     * specified.
     *
     * If the element already exists in the datastore, then it should be replaced with the
     * value provided.
     *
     * @param key Identifier for the value being stored
     * @param id Identifier for the cluster member where this value should be stored
     * @param value The value being stored
     * @param ttl Duration to store the value for
     * @param timeUnit unit of time {@link java.util.concurrent.TimeUnit} that the ttl is defined in
     * @param remoteBehavior Whether or not to allow remote storage of this value
     * @throws com.rackspace.papi.components.datastore.DatastoreOperationException if an exception occurs when
     *         attempting to store the value
     */
    public void put(String key, byte[] id, final byte[] value, final int ttl, final TimeUnit timeUnit,
                    RemoteBehavior remoteBehavior) throws DatastoreOperationException;

    /**
     * Removes a StoredElement from the Datastore.
     *
     * @param key
     * @param id Identifier for the cluster member where this value should be stored
     * @param remoteBehavior Whether or not to allow remote removal of this value
     * @return true if the element was removed, false if it was not found in the Datastore
     * @throws DatastoreOperationException if an exception occurs when attempting to remove the
     * stored value
     */
    public boolean remove(String key, byte[] id, RemoteBehavior remoteBehavior);

    /**
     * Gets a StoredElement from the Datastore.  If there is no value found for the
     * provided key, a StoredElement object should be returned containing a null or empty element.
     *
     * @param key Key provided when the data was stored
     * @param id Identifier for the cluster member where this value should be stored
     * @param remoteBehavior Whether or not to allow remote retrieval of this value
     * @return StoredElement wrapper of the stored value
     * @throws DatastoreOperationException if an exception occurs when attempting to retrieve the
     * stored value
     */
    public StoredElement get(String key, byte[] id, RemoteBehavior remoteBehavior);

}
