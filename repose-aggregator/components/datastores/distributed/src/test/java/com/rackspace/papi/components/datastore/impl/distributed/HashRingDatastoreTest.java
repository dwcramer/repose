package com.rackspace.papi.components.datastore.impl.distributed;

import com.rackspace.papi.commons.util.encoding.UUIDEncodingProvider;
import com.rackspace.papi.components.datastore.Datastore;
import com.rackspace.papi.components.datastore.DatastoreOperationException;
import com.rackspace.papi.components.datastore.StoredElementImpl;
import com.rackspace.papi.components.datastore.distributed.ClusterView;
import com.rackspace.papi.components.datastore.distributed.RemoteBehavior;
import com.rackspace.papi.components.datastore.hash.MD5MessageDigestFactory;
import com.rackspace.papi.components.datastore.impl.distributed.remote.RemoteCommandExecutor;
import com.rackspace.papi.components.datastore.impl.distributed.remote.RemoteConnectionException;
import com.rackspace.papi.components.datastore.impl.distributed.remote.command.Get;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.mockito.Mockito.*;

/**
 * A unit test suite for the Hash Ring Datastore
 */
@RunWith(Enclosed.class)
public class HashRingDatastoreTest {

    public static class WhenPerformingAction {

        protected Datastore localDatastore;
        protected ClusterView clusterView;
        protected StoredElementImpl storedElement;
        protected DatastoreAction datastoreAction;
        protected HashRingDatastore hashRingDatastore;
        protected RemoteCommandExecutor remoteCommandExecutor;

        private InetSocketAddress inetSocketAddress;

        @Before
        public void standUp() throws Exception {
            inetSocketAddress = new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 1, 1, 11}), 2200);

            localDatastore = mock(Datastore.class);
            clusterView = mock(ClusterView.class);
            storedElement = mock(StoredElementImpl.class);
            datastoreAction = mock(DatastoreAction.class);
            remoteCommandExecutor = mock(RemoteCommandExecutor.class);

            when(clusterView.hasDamagedMembers()).thenReturn(false);
            when(storedElement.elementIsNull()).thenReturn(new Boolean(false));
            when(storedElement.elementAs(Boolean.class)).thenReturn(new Boolean(false));

            hashRingDatastore = spy(new HashRingDatastore(remoteCommandExecutor, clusterView, "", localDatastore,
                    MD5MessageDigestFactory.getInstance(), UUIDEncodingProvider.getInstance()));
        }

        @Test
        public void shouldCatchDOExceptionFromIsRemoteTarget() throws Exception {
            when(clusterView.members()).thenReturn(
                    new InetSocketAddress[]{inetSocketAddress}, new InetSocketAddress[]{});
            when(localDatastore.get(any(String.class))).thenThrow(new DatastoreOperationException("")).thenReturn(storedElement);

            hashRingDatastore.get("", new byte[]{0}, RemoteBehavior.ALLOW_FORWARDING);

            verify(remoteCommandExecutor, never()).execute(any(Get.class), any(RemoteBehavior.class));
            verify(clusterView).memberDamaged(eq(inetSocketAddress), any(String.class));
        }

        @Test
        public void shouldCatchDOExceptionFromPerformRemote() throws Exception {
            when(clusterView.members()).thenReturn(
                    new InetSocketAddress[]{inetSocketAddress}, new InetSocketAddress[]{});
            when(localDatastore.get(any(String.class))).thenReturn(storedElement);
            when(remoteCommandExecutor.execute(any(Get.class), any(RemoteBehavior.class))).
                    thenThrow(new DatastoreOperationException(""));

            hashRingDatastore.get("", new byte[]{0}, RemoteBehavior.ALLOW_FORWARDING);

            verify(clusterView).memberDamaged(eq(inetSocketAddress), any(String.class));
        }

        @Test
        public void shouldCatchRCExceptionFromPerformRemote() throws Exception {
            when(clusterView.members()).thenReturn(
                    new InetSocketAddress[]{inetSocketAddress}, new InetSocketAddress[]{});
            when(localDatastore.get(any(String.class))).thenReturn(storedElement);
            when(remoteCommandExecutor.execute(any(Get.class), any(RemoteBehavior.class))).
                    thenThrow(new RemoteConnectionException("", new RuntimeException()));

            hashRingDatastore.get("", new byte[]{0}, RemoteBehavior.ALLOW_FORWARDING);

            verify(clusterView).memberDamaged(eq(inetSocketAddress), any(String.class));
        }
    }
}
