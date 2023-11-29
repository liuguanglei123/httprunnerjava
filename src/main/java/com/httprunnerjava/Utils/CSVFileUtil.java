package com.httprunnerjava.utils;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class CSVFileUtil {
    /**
     * CSV文件编码
     */
    private static final String ENCODE = "UTF-8";

    /**
     * 读取CSV文件得到List，默认使用UTF-8编码
     * @param fileName 文件路径
     */
    public static List<String> getLines(File fileName) {
        return getLines(fileName, ENCODE);
    }

    public static List<String> getLines(String fileName) {
        File file = new File("classpath:fileName");
        return getLines(file, ENCODE);
    }


    public static List<String> getLines(InputStream inputStream, String encode){
        List<String> lines = new ArrayList<String>();
        BufferedReader br = null;
        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(inputStream, encode);
            br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                StringBuilder sb = new StringBuilder();
                sb.append(line);
                boolean readNext = countChar(sb.toString(), '"', 0) % 2 == 1;
                // 如果双引号是奇数的时候继续读取。考虑有换行的是情况
                while (readNext) {
                    line = br.readLine();
                    if (line == null) {
                        return null;
                    }
                    sb.append(line);
                    readNext = countChar(sb.toString(), '"', 0) % 2 == 1;
                }
                lines.add(sb.toString());
            }
        } catch (Exception e) {
            log.error("Read CSV file failure :", e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (isr != null) {
                    isr.close();
                }
            } catch (IOException e) {
                log.error("Close stream failure :%s", e);
            }
        }
        return lines;
    }

    /**
     * 读取CSV文件得到List
     * @param fileName 文件路径
     * @param encode 编码
     */
    public static List<String> getLines(File fileName, String encode) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileName);
            return getLines(fis,encode);
        } catch (Exception e) {
            log.error("Read CSV file failure :%s", e);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                log.error("Close stream failure :%s", e);
            }
        }

        return new ArrayList<>();
    }

    public static String[] fromCSVLine(String source) {
        return fromCSVLine(source, 0);
    }

    /**
     * 把CSV文件的一行转换成字符串数组。指定数组长度，不够长度的部分设置为null
     */
    public static String[] fromCSVLine(String source, int size) {
        List<String> list = fromCSVLineToArray(source);
        if (size < list.size()) {
            size = list.size();
        }
        String[] arr = new String[size];
        list.toArray(arr);
        return arr;
    }

    public static List<String> fromCSVLineToArray(String source) {
        if (source == null || source.length() == 0) {
            return new ArrayList<>();
        }
        int currentPosition = 0;
        int maxPosition = source.length();
        int nextComa = 0;
        List<String> list = new ArrayList<>();
        while (currentPosition < maxPosition) {
            nextComa = nextComma(source, currentPosition);
            list.add(nextToken(source, currentPosition, nextComa));
            currentPosition = nextComa + 1;
            if (currentPosition == maxPosition) {
                list.add("");
            }
        }
        return list;
    }

    /**
     * 把字符串类型的数组转换成一个CSV行。（输出CSV文件的时候用）
     */
    public static String toCSVLine(String[] arr) {
        if (arr == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            String item = addQuote(arr[i]);
            sb.append(item);
            if (arr.length - 1 != i) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * 将list的第一行作为Map的key，下面的列作为Map的value
     */
    public static List<Map<String, String>> parseListoMap(List<String> list) {
        List<Map<String, String>> resultList = new ArrayList<>();
        String firstLine = list.get(0);
        String[] fields = firstLine.split(",");
        for (int i = 1; i < list.size(); i++) {
            String valueLine = list.get(i);
            if(valueLine.contains("tag")){
                continue;
            }
            String[] valueItems = CSVFileUtil.fromCSVLine(valueLine);
            Map<String, String> map = new HashMap<>();
            try {
                for (int j = 0; j < fields.length; j++) {
                        map.put(fields[j], valueItems[j]);
                }
            }catch (Exception e){
                log.error("加载的内容出错了，出错的行内容为" + JSON.toJSONString(valueItems) + ",改行将跳过",e);
                continue;
            }
            resultList.add(map);
        }
        return resultList;
    }

    /**
     * 字符串类型的List转换成一个CSV行。（输出CSV文件的时候用）
     */
    public static String toCSVLine(ArrayList<String> strArrList) {
        if (strArrList == null) {
            return "";
        }
        String[] strArray = new String[strArrList.size()];
        for (int idx = 0; idx < strArrList.size(); idx++) {
            strArray[idx] = strArrList.get(idx);
        }
        return toCSVLine(strArray);
    }

    /**
     * 计算指定字符的个数
     *
     * @param str   文字列
     * @param c     字符
     * @param start 开始位置
     * @return 个数
     */
    private static int countChar(String str, char c, int start) {
        int index = str.indexOf(c, start);
        return index == -1 ? 0 : countChar(str, c, index + 1) + 1;
    }

    /**
     * 查询下一个逗号的位置。
     *
     * @param source 文字列
     * @param st     检索开始位置
     * @return 下一个逗号的位置。
     */
    private static int nextComma(String source, int st) {
        int maxPosition = source.length();
        boolean inquote = false;
        while (st < maxPosition) {
            char ch = source.charAt(st);
            if (!inquote && ch == ',') {
                break;
            } else if ('"' == ch) {
                inquote = !inquote;
            }
            st++;
        }
        return st;
    }

    /**
     * 取得下一个字符串
     */
    private static String nextToken(String source, int st, int nextComma) {
        StringBuilder strb = new StringBuilder();
        int next = st;
        while (next < nextComma) {
            char ch = source.charAt(next++);
            if (ch == '"') {
                if ((st + 1 < next && next < nextComma) && (source.charAt(next) != '"')) {
                    strb.append(ch);
                } else if ((st + 1 < next && next < nextComma) && (source.charAt(next) == '"')) {
                    strb.append(ch);
                    next++;
                }
            } else {
                strb.append(ch);
            }
        }
        return strb.toString();
    }

    /**
     * 在字符串的外侧加双引号。如果该字符串的内部有双引号的话，把"转换成""。
     *
     * @param item 字符串
     * @return 处理过的字符串
     */
    private static String addQuote(String item) {
        if (item == null || item.length() == 0) {
            return "\"\"";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int idx = 0; idx < item.length(); idx++) {
            char ch = item.charAt(idx);
            if ('"' == ch) {
                sb.append("\"\"");
            } else {
                sb.append(ch);
            }
        }
        sb.append('"');
        return sb.toString();
    }

//    private static String addQuote(String item) {
//        if (item == null || item.length() == 0) {
//            return "\"\"";
//        }
//        StringBuilder sb = new StringBuilder();
//        sb.append('"');
//        for (int idx = 0; idx < item.length(); idx++) {
//            char ch = item.charAt(idx);
//            if ('"' == ch) {
//                sb.append("\"\"");
//            } else {
//                sb.append(ch);
//            }
//        }
//        sb.append('"');
//        return sb.toString();
//    }

}