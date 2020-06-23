package nikonov.torrentclient.client.download.domain;

import lombok.*;

@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class Block {
    private int index;
    private int begin;
    private int length;
}
