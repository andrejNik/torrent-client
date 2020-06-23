package nikonov.torrentclient.client.metadata;

import nikonov.torrentclient.client.metadata.domain.BencodeItem;

import java.nio.charset.Charset;
import java.util.*;

/**
 * Парсер bencode-данных
 */
public class BencodeReader {

    private String bencodeContent;

    public BencodeReader(byte[] content) {
        this.bencodeContent = new String(content, Charset.forName("ascii"));
    }

    public List<BencodeItem> content() {
        var list = new ArrayList<BencodeItem>();
        var current = 0;
        while(current < bencodeContent.length()) {
            var item = item(current);
            list.add(item);
            current = item.getIndexEnd() + 1;
        }
        return list;
    }

    private BencodeItem map(int indexStart) {
        var map = new HashMap<String, BencodeItem>();
        var current = indexStart + 1;
        while( !indexE(current) ) {
            var stringItem = string(current);
            var valueItem = item(stringItem.getIndexEnd() + 1);
            map.put((String)stringItem.getValue(), valueItem);
            current = valueItem.getIndexEnd() + 1;
        }
        var result = new BencodeItem();
        result.setIndexStart(indexStart);
        result.setIndexEnd(current);
        result.setValue(map);
        return result;
    }

    private BencodeItem list(int indexStart) {
        var list = new ArrayList<BencodeItem>();
        var current = indexStart + 1;
        while( !indexE(current)) {
            var item = item(current);
            list.add(item);
            current = item.getIndexEnd() + 1;
        }
        var result = new BencodeItem();
        result.setIndexStart(indexStart);
        result.setIndexEnd(current);
        result.setValue(list);
        return result;
    }

    private BencodeItem number(int indexStart) {
        var indexE = bencodeContent.indexOf('e', indexStart);
        return new BencodeItem(indexStart, indexE, Long.valueOf(bencodeContent.substring(indexStart+1, indexE)));
    }

    private BencodeItem string(int indexStart) {
        var indexColon = bencodeContent.indexOf(':', indexStart);
        var length = Integer.valueOf(bencodeContent.substring(indexStart, indexColon));
        var indexEnd = indexColon + length;
        /*
         * indexColon + 1 - для строки indexStart указывает на первый байт значения строки
         * а для всех остальных указывается служебный символ. напрмер, для словаря indexStart указывает на 'd'
         */
        return new BencodeItem(indexColon + 1, indexEnd, bencodeContent.substring(indexColon+1, indexEnd+1));
    }


    private BencodeItem item(int indexStart) {
        switch (bencodeContent.charAt(indexStart)) {
            case 'd':
                return map(indexStart);
            case 'l':
                return list(indexStart);
            case 'i':
                return number(indexStart);
            default:
                return string(indexStart);
        }
    }

    private boolean indexE(int index) {
        return bencodeContent.charAt(index) == 'e';
    }
}
