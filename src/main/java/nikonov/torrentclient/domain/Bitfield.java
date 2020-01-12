package nikonov.torrentclient.domain;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.text.MessageFormat.format;

public class Bitfield {

    private final Map<Integer, Boolean> map;

    public Bitfield(int countPieces) {
        if ((countPieces % 8) != 0) {
            /*
             * округление вверх countPieces до ближайшего целого, кратного 8
             * это делается для удобства сравнения ( difference ) битовой маски текущего клиента и удаленного пира
             * тк по сети от пира придет маска размером кратным 8
             */
            countPieces = 8 * ((countPieces / 8) + 1);
        }
        map = new ConcurrentHashMap<>();
        for(var i = 0; i < countPieces;i++) {
            map.put(i, false);
        }
    }

    public int size() {
        return map.size();
    }

    public boolean isHave(int index) {
        checkIndex(index);
        return map.get(index);
    }

    public void have(int index) {
        checkIndex(index);
        map.put(index, true);
    }

    private void checkIndex(int index) {
        var maxIndex = size() - 1;
        if (index > maxIndex) {
            throw new IllegalArgumentException(format("индекс {0} за пределами допусимого значения {1}", index, maxIndex));
        }
    }
}
