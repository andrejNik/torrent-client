package nikonov.torrentclient.metadata;

import nikonov.torrentclient.metadata.domain.metadata.TrackerAnnounce;
import nikonov.torrentclient.metadata.domain.metadata.Metadata;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import  org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static nikonov.torrentclient.metadata.domain.metadata.TrackerProtocol.UDP;

@RunWith(MockitoJUnitRunner.class)
public class TorrentFileParserTest {

    private Metadata metadata;

    @Before
    public void setUp() throws Exception {
        var classLoader = TorrentFileParserTest.class.getClassLoader();
        /*var pathTorrentFile = Objects.requireNonNull(
                classLoader.getResource("nikonov/torrentclient/metadata/stranger-things.torrent")).getPath();*/
        var pathTorrentFile = "C:/Users/andre/Desktop/torrent/torrentclient/target/test-classes/nikonov/torrentclient/metadata/stranger-things.torrent";
        metadata = new TorrentFileParser().metadata(pathTorrentFile);
    }

    @Test
    public void announces() {
        var actual = metadata.getTrackerAnnounces();
        var expect = List.of(
                new TrackerAnnounce(UDP, "tracker.opentrackr.org", 1337, "/announce")
        );
    }
}