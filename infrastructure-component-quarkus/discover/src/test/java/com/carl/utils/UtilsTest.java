package com.carl.utils;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class UtilsTest {
    @Test
    public void test() throws UnknownHostException {
        InetAddress localHost = InetAddress.getLocalHost();

        // 获取主机名
        String hostName = localHost.getHostName();
        System.out.println("主机名: " + hostName);

        // 获取IP地址
        String hostAddress = localHost.getHostAddress();
        System.out.println("IP地址: " + hostAddress);
    }
}
