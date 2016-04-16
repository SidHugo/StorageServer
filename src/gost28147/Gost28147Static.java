/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gost28147;

import javax.xml.bind.DatatypeConverter;
import java.nio.ByteBuffer;


/**
 *
 * @author Роман
 */
public class Gost28147Static {

    //таблица замен
    private static final byte[][] sBox = {
        {0x04, 0x0a, 0x09, 0x02, 0x0d, 0x08, 0x00, 0x0e, 0x06, 0x0B, 0x01, 0x0c, 0x07, 0x0f, 0x05, 0x03},
        {0x0e, 0x0b, 0x04, 0x0c, 0x06, 0x0d, 0x0f, 0x0a, 0x02, 0x03, 0x08, 0x01, 0x00, 0x07, 0x05, 0x09},
        {0x05, 0x08, 0x01, 0x0d, 0x0a, 0x03, 0x04, 0x02, 0x0e, 0x0f, 0x0c, 0x07, 0x06, 0x00, 0x09, 0x0b},
        {0x07, 0x0d, 0x0a, 0x01, 0x00, 0x08, 0x09, 0x0f, 0x0e, 0x04, 0x06, 0x0c, 0x0b, 0x02, 0x05, 0x03},
        {0x06, 0x0c, 0x07, 0x01, 0x05, 0x0f, 0x0d, 0x08, 0x04, 0x0a, 0x09, 0x0e, 0x00, 0x03, 0x0b, 0x02},
        {0x04, 0x0b, 0x0a, 0x00, 0x07, 0x02, 0x01, 0x0d, 0x03, 0x06, 0x08, 0x05, 0x09, 0x0c, 0x0f, 0x0e},
        {0x0d, 0x0b, 0x04, 0x01, 0x03, 0x0f, 0x05, 0x09, 0x00, 0x0a, 0x0e, 0x07, 0x06, 0x08, 0x02, 0x0c},
        {0x01, 0x0f, 0x0d, 0x00, 0x05, 0x07, 0x0a, 0x04, 0x09, 0x02, 0x03, 0x0e, 0x06, 0x0b, 0x08, 0x0c}
    };
    //массив ключей
    private static final String[] k = {"733D2C20", "65686573", "74746769", "79676120", "626E7373", "20657369", "326C6568", "33206D54"};
    
    //синхропосылка
    private static final String synchMsg = "5C713FA2D7B58429";
    
    /**
     * Шифрование методом гаммирования с обратной связью
     * @param data исходные данные
     * @return зашифрованные данные
     */
    public static byte[] gammaEncrypt(byte[] data){
        int count = (int) Math.ceil(data.length / 8) + 1;
        byte[] forE32;
        forE32 = toByteArray(synchMsg);
        byte[] res = new byte[data.length];
        for (int i = 0; i < count - 1; ++i){  //цикл шифрования данных по 8 байт
            forE32 = e32(forE32);
            byte[] buf = new byte[8];
            System.arraycopy(data, i * 8, buf, 0, 8);
            for (int j = 0; j < 8; ++j) {
                forE32[j] = (byte) (forE32[j] ^ buf[j]);
            }
            System.arraycopy(forE32, 0, res, i * 8, 8);
        }
        int tailLength = data.length - (count - 1) * 8; //длина оставнихся данных
        if (tailLength > 0){        //шифрование оставшихся данных
            forE32 = e32(forE32);
            byte[] buf = new byte[8];
            System.arraycopy(data, (count - 1) * 8, buf, 0, tailLength);
            for (int j = 0; j < 8; ++j) {
                forE32[j] = (byte) (forE32[j] ^ buf[j]);
            }
            System.arraycopy(forE32, 0, res, (count - 1) * 8, tailLength);
        }
        return res;
    }
    
    /**
     * Расшифрование методом гаммирования с обратной связью
     * @param data зашифрованные данные
     * @return расшифрованные данные
     */
    public static byte[] gammaDecrypt(byte[] data){
        int count = (int) Math.ceil(data.length / 8) + 1;
        byte[] forE32;
        forE32 = toByteArray(synchMsg);
        byte[] res = new byte[data.length];
        for (int i = 0; i < count - 1; ++i){    //основнойй цикл расшифрования
            forE32 = e32(forE32);               //аналогичен шифрованию
            byte[] buf = new byte[8];
            System.arraycopy(data, i * 8, buf, 0, 8);
            for (int j = 0; j < 8; ++j) {
                forE32[j] = (byte) (forE32[j] ^ buf[j]);
            }
            System.arraycopy(forE32, 0, res, i * 8, 8);
            System.arraycopy(data, i * 8, forE32, 0, 8);//новый блок зашифрованных данных
        }
        int tailLength = data.length - (count - 1) * 8;
        if (tailLength > 0){        //расшифрование оставшихсяя данных
            forE32 = e32(forE32);
            byte[] buf = new byte[8];
            System.arraycopy(data, (count - 1) * 8, buf, 0, tailLength);
            for (int j = 0; j < 8; ++j) {
                forE32[j] = (byte) (forE32[j] ^ buf[j]);
            }
            System.arraycopy(forE32, 0, res, (count - 1) * 8, tailLength);
        }
        return res;
    }
    /**
     * Операция базового шага по алгоритму ГОСТ-28147
     * @param data данные
     * @param keyNumber номер ключа из массива
     * @return результат выполнения базового шага
     */
    private static byte[] basicStep(byte[] data, int keyNumber) {
        byte[] data1 = new byte[4];
        byte[] data2 = new byte[4];
        System.arraycopy(data, 0, data1, 0, 4);
        System.arraycopy(data, 4, data2, 0, 4);
        //byte[] buf;
        String strKey = k[keyNumber];
        String strData2 = toHexString(data2);
        int sumRes = (int) (Long.parseLong(strKey, 16) + Long.parseLong(strData2, 16));
        //String strSumRes = Integer.toHexString(sumRes);
        //strSumRes = complete(strSumRes, 8);
        //buf = toByteArray(strSumRes);
        byte[] buf = ByteBuffer.allocate(4).putInt(sumRes).array();
        buf = replacement(buf);
        //String strBuf = toHexString(buf);
        //int intBuf = (int) Long.parseLong(strBuf, 16);
        ByteBuffer wrapped = ByteBuffer.wrap(buf);
        int rotatedBuf = wrapped.getInt();
        rotatedBuf = (int) Integer.rotateLeft(rotatedBuf, 11);
        //strBuf = Integer.toHexString(intBuf);
        //strBuf = complete(strBuf, 8);
        //buf = toByteArray(strBuf);
        buf = ByteBuffer.allocate(4).putInt(rotatedBuf).array();
        for (int i = 0; i < 4; ++i) {
            buf[i] = (byte) (buf[i] ^ data1[i]);
        }
        byte[] res = new byte[8];
        System.arraycopy(data2, 0, res, 0, 4);
        System.arraycopy(buf, 0, res, 4, 4);
        return res;
    }
    /**
     * Операция замены в соответсвии с таблицей замен
     * @param data данные
     * @return результат операции
     */
    private static byte[] replacement(byte[] data) {
        byte[] res = new byte[4];
        for (int i = 0; i < data.length; ++i) {
            byte bufH = (byte) ((data[i] & 0xf0) >> 4);
            byte bufL = (byte) (data[i] & 0x0f);
            bufH = sBox[i * 2][bufH];
            bufL = sBox[i * 2 + 1][bufL];
            res[i] = (byte) ((bufH << 4) + bufL);
        }
        return res;
    }
    /**
     * Цикл 32-З по алгоритму ГОСТ-28147
     * @param data данные размером 8 байт
     * @return зашифрованные данные
     */
    public static byte[] e32(byte[] data) {
        data = basicStep(data, 0);
        data = basicStep(data, 1);
        data = basicStep(data, 2);
        data = basicStep(data, 3);
        data = basicStep(data, 4);
        data = basicStep(data, 5);
        data = basicStep(data, 6);
        data = basicStep(data, 7);

        data = basicStep(data, 0);
        data = basicStep(data, 1);
        data = basicStep(data, 2);
        data = basicStep(data, 3);
        data = basicStep(data, 4);
        data = basicStep(data, 5);
        data = basicStep(data, 6);
        data = basicStep(data, 7);

        data = basicStep(data, 0);
        data = basicStep(data, 1);
        data = basicStep(data, 2);
        data = basicStep(data, 3);
        data = basicStep(data, 4);
        data = basicStep(data, 5);
        data = basicStep(data, 6);
        data = basicStep(data, 7);

        data = basicStep(data, 7);
        data = basicStep(data, 6);
        data = basicStep(data, 5);
        data = basicStep(data, 4);
        data = basicStep(data, 3);
        data = basicStep(data, 2);
        data = basicStep(data, 1);
        data = basicStep(data, 0);

        swap(data);

        return data;
    }
    /**
     * Цикл 32-Р в соответствии с алгоритмом ГОСТ-28147
     * @param data зашифрованные данные
     * @return расшифрованные данные
     */
    public static byte[] d32(byte[] data) {
        data = basicStep(data, 0);
        data = basicStep(data, 1);
        data = basicStep(data, 2);
        data = basicStep(data, 3);
        data = basicStep(data, 4);
        data = basicStep(data, 5);
        data = basicStep(data, 6);
        data = basicStep(data, 7);

        data = basicStep(data, 7);
        data = basicStep(data, 6);
        data = basicStep(data, 5);
        data = basicStep(data, 4);
        data = basicStep(data, 3);
        data = basicStep(data, 2);
        data = basicStep(data, 1);
        data = basicStep(data, 0);

        data = basicStep(data, 7);
        data = basicStep(data, 6);
        data = basicStep(data, 5);
        data = basicStep(data, 4);
        data = basicStep(data, 3);
        data = basicStep(data, 2);
        data = basicStep(data, 1);
        data = basicStep(data, 0);

        data = basicStep(data, 7);
        data = basicStep(data, 6);
        data = basicStep(data, 5);
        data = basicStep(data, 4);
        data = basicStep(data, 3);
        data = basicStep(data, 2);
        data = basicStep(data, 1);
        data = basicStep(data, 0);

        swap(data);

        return data;
    }
    /**
     * Перевод массива байт в строку
     * @param array исходный массив
     * @return строка
     */
    public static String toHexString(byte[] array) {
        return DatatypeConverter.printHexBinary(array);
    }
    /**
     * Перевод строки в массив байт
     * @param s исходная строка
     * @return массив
     */
    public static byte[] toByteArray(String s) {
        return DatatypeConverter.parseHexBinary(s);
    }
    /**
     * Дополнение строки до нужного количества байт
     * @param s исходныя строка
     * @param num количество байт
     * @return результат операции
     */
    private static String complete(String s, int num) {
        while (s.length() != num) {
            s = "0" + s;
        }
        return s;
    }
    /**
     * Операция замены старшей и младшей части массива байт
     * @param data 
     */
    private static void swap(byte[] data) {
        byte buf;
        for (int i = 0; i < 4; ++i) {
            buf = data[i];
            data[i] = data[i + 4];
            data[i + 4] = buf;
        }
    }
}
