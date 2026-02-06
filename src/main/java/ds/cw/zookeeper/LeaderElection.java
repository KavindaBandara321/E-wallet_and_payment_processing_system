package ds.cw.zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import java.io.IOException;

public class LeaderElection implements Watcher {

    private static final String ZK_ADDRESS = "localhost:2181";
    private final String electionPath;
    private ZooKeeper zooKeeper;

    public LeaderElection(String shardId) {
        this.electionPath = "/wallet/shards/" + shardId + "/leader";
    }

    public void connect() throws IOException {
        zooKeeper = new ZooKeeper(ZK_ADDRESS, 3000, this);
    }

    private void ensureParentPaths() throws KeeperException, InterruptedException {
        String[] paths = electionPath.split("/");
        StringBuilder currentPath = new StringBuilder();
        for (int i = 1; i < paths.length - 1; i++) {
            currentPath.append("/").append(paths[i]);
            if (zooKeeper.exists(currentPath.toString(), false) == null) {
                try {
                    zooKeeper.create(currentPath.toString(), new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,
                            CreateMode.PERSISTENT);
                } catch (KeeperException.NodeExistsException e) {
                    // Ignore if created by another server concurrently
                }
            }
        }
    }

    public synchronized void awaitLeadership() throws KeeperException, InterruptedException {
        ensureParentPaths();
        while (true) {
            try {
                zooKeeper.create(
                        electionPath,
                        "leader".getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.EPHEMERAL);
                System.out.println("This server is the LEADER for " + electionPath);
                return;
            } catch (KeeperException.NodeExistsException e) {
                System.out.println("Leader already exists for " + electionPath + ". Waiting...");
                // Wait for the leader node to be deleted
                Stat stat = zooKeeper.exists(electionPath, true);
                if (stat != null) {
                    wait(); // Wait for the watcher to notify us
                }
            }
        }
    }

    @Override
    public synchronized void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.NodeDeleted && electionPath.equals(event.getPath())) {
            notifyAll(); // Wake up the thread waiting in awaitLeadership
        }
    }
}
