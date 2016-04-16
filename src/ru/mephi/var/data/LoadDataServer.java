/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.mephi.var.data;

/**
 * Главный класс, реализующий запуск сервера хранения
 * @author Роман
 */
public class LoadDataServer {
    public static void main(String[] args) {
    	if (args.length != 1) {
    		System.out.println("Wrong parameters");
    		return;
    	} else {
    		String serverIP = args[0];
    		DataServer dataServer = new DataServer();
    		dataServer.read(serverIP);
    	}
    }
}
