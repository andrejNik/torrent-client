package nikonov.torrentclient.util;

import nikonov.torrentclient.metadata.domain.metadata.Metadata;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DomainUtil {

    /**
     * Сформировать карту вида полное индекс файла -> [индекс первого байта, индекс последнего байта]
     */
    public static Map<Integer, long[]> fileBorderMap(Metadata metadata) {
        var map = new HashMap<Integer, long[]>();
        var current = 0L;
        for(var i = 0; i < metadata.getInfo().getFiles().size(); i++) {
            var file = metadata.getInfo().getFiles().get(i);
            var startFile = current;
            var endFile = current + file.getLength() - 1;
            current += file.getLength();
            map.put(i, new long[]{startFile, endFile});
        }
        return Collections.unmodifiableMap( map );
    }
}
