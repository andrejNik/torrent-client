package nikonov.torrentclient.download.domain;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class DownloadBlock {
    private int index;
    private int begin;
    private int length;
}
