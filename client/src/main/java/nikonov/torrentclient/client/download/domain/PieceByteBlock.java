package nikonov.torrentclient.client.download.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PieceByteBlock {
    private int begin;
    private byte[] block;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PieceByteBlock that = (PieceByteBlock) o;
        return begin == that.begin;
    }

    @Override
    public int hashCode() {
        return Objects.hash(begin);
    }
}
