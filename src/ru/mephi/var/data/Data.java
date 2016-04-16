/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.mephi.var.data;

/**
 * Элемент таблицы данных
 * @author Роман
 */
public class Data {
    private byte[] data;
    private byte type;
    private int length;
    
    /**
    * Конструктор
    * @param data массив байт с данными
    * @param type тип данных
    * @param length длинна данных
    */
    public Data(byte[] data, byte type, int length){
        this.data = data;
        this.type = type;
        this.length = length;
    }
    
    public Data(){}
    
    public byte[] getData(){
        return this.data;
    }
    
    public void setData(byte[] data){
        this.data = data;
    }
    
    public byte getType(){
        return this.type;
    }
    
    public void setType(byte type){
        this.type = type;
    }
    
    public int getLength(){
        return this.length;
    }
    
    public void setLength(int length){
        this.length = length;
    }
}
