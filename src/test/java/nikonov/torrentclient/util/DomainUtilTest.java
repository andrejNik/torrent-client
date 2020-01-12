package nikonov.torrentclient.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DomainUtilTest {

    @Test
    public void fileBlockList() {
        /*var metadata = new Metadata();
        var info = new Info();
        info.setPieceLength(5);
        info.setFiles(List.of(
                new File(7, singletonList("1.txt")),
                new File(11, singletonList("2.txt")),
                new File(6, singletonList("3.txt"))));
        metadata.setInfo(info);

        var fileBlockList = DomainUtil.fileBlockList(metadata, 3);
        Assert.assertEquals(3, fileBlockList.size());
        var map = fileBlockList.stream().collect(Collectors.toMap(FileBlock::getFilename, Function.identity()));
        assertFileBlock(
                new FileBlock("1.txt", List.of(new Block(0, 0, 3), new Block(0, 3, 2), new Block(1, 0, 3))),
                map.get("1.txt")
        );
        assertFileBlock(
                new FileBlock("2.txt", List.of(
                        new Block(1, 0, 3), new Block(1, 3, 2),
                        new Block(2, 0, 3), new Block(2, 3, 2),
                        new Block(3, 0, 3))),
                map.get("2.txt")
        );
        assertFileBlock(
                new FileBlock("3.txt", List.of(
                        new Block(3, 3, 2),
                        new Block(4, 0, 3),
                        new Block(4, 3, 1))),
                map.get("3.txt")
        );*/
    }
}
