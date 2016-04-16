package ru.mephi.var.data.tests;

import ru.mephi.var.data.Data;
import ru.mephi.var.data.StohasticTree;
import ru.mephi.var.data.Storage;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by Sid_Hugo on 16.04.2016.
 */
public class HashVsTreeTest {
    public static void test(int amount) {
        Storage hashStorage=new Storage();
        StohasticTree stohasticTree=new StohasticTree(null);

        byte[] data="data123".getBytes();
        byte type=1;
        int length=data.length;

        testPut(amount, hashStorage, stohasticTree, data, type, length);
        testGetLast(amount, hashStorage, stohasticTree);
        testGet(amount, hashStorage, stohasticTree);
    }

    public static void testGet(int amount, Storage hashStorage, StohasticTree stohasticTree) {
        long startTime=System.currentTimeMillis();
        for(int i=0; i<amount; ++i) {
            hashStorage.get(new UUID(0, i));
        }
        double hashGetTime=System.currentTimeMillis()-startTime;

        startTime=System.currentTimeMillis();
        for(int i=0; i<amount; ++i) {
            stohasticTree.get(new UUID(0, i));
        }
        double treeGetTime=System.currentTimeMillis()-startTime;
        System.out.println("===========GET===========");
        System.out.println("HashMap time:\t"+hashGetTime);
        System.out.println("Tree time:\t"+treeGetTime);
        System.out.println("TreeTime/HashMapTime=:\t"+(treeGetTime/hashGetTime));
    }

    public static void testGetLast(int amount, Storage hashStorage, StohasticTree stohasticTree) {
        long startTime;

        startTime=System.currentTimeMillis();
        Data data1=hashStorage.get(new UUID(0, amount-1));
        double hashGetLastTime=System.currentTimeMillis()-startTime;

        data1=null;
        startTime=System.currentTimeMillis();
        data1=stohasticTree.get(new UUID(0, amount-1));
        double treeGetLastTime=System.currentTimeMillis()-startTime;

        System.out.println("===========GettingLast===========");
        System.out.println("HashMap time:\t" + hashGetLastTime);
        System.out.println("Tree time:\t"+treeGetLastTime);
        System.out.println("TreeTime/HashMapTime=:\t" + (treeGetLastTime/hashGetLastTime));
    }

    public static void testPut(int amount, Storage hashStorage, StohasticTree stohasticTree, byte[] data, byte type, int length) {
        long startTime=System.currentTimeMillis();
        for(int i=0; i<amount; ++i) {
            hashStorage.put(new UUID(0, i), new Data(data, type, length));
        }
        double hashPutTime=System.currentTimeMillis()-startTime;

        startTime=System.currentTimeMillis();
        for(int i=0; i<amount; ++i) {
            stohasticTree.put(new UUID(0, i), new Data(data, type, length));
        }
        double treePutTime=System.currentTimeMillis()-startTime;
        System.out.println("===========PUT===========");
        System.out.println("HashMap time:\t"+hashPutTime);
        System.out.println("Tree time:\t"+treePutTime);
        System.out.println("TreeTime/HashMapTime=:\t"+(treePutTime/hashPutTime));
    }
}
