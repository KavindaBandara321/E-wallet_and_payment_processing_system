package ds.cw.zookeeper;

import org.apache.zookeeper.*;
import java.io.IOException;

public class LeaderElection implements Watcher {

    private static final String ZK_ADDRESS = "localhost:2181";
    private static final String ELECTION_PATH = "/wallet-leader";
    private ZooKeeper zooKeeper;

    public void connect() throws IOException {
        zooKeeper = new ZooKeeper(ZK_ADDRESS, 3000, this);
    }

    public boolean attemptLeadership() throws KeeperException, InterruptedException {
        try {
            zooKeeper.create(
                    ELECTION_PATH,
                    "leader".getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL
            );
            System.out.println("This server is the LEADER");
            return true;
        } catch (KeeperException.NodeExistsException e) {
            System.out.println("Leader already exists. This server is FOLLOWER");
            return false;
        }
    }

    @Override
    public void process(WatchedEvent event) {}
}
