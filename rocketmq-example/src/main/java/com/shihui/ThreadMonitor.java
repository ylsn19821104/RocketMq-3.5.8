package com.shihui;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @Description:监控tomcat线程
 */
public class ThreadMonitor {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private final static Integer delay = 1000;
    private final static Integer defaultPeriod = 30000;

    public ThreadMonitor(String url, String httpPort) {
        this(url, httpPort, null, null);
    }

    public ThreadMonitor(String url, String httpPort, Integer period) {
        this(url, httpPort, null, null, period);
    }

    public ThreadMonitor(String url, String httpPort, String username, String pwd) {
        this(url, httpPort, username, pwd, defaultPeriod);
    }

    public ThreadMonitor(final String url, final String httpPort, final String username, final String pwd,
                         final Integer period) {

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                logger.info("ThreadMonitor start run ,url :{},httpPort:{},username:{},pwd:{},period:{}", url, httpPort,
                        username, pwd, period);

                JMXConnector jmxConnector = null;

                try {
                    JMXServiceURL ServiceURL = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + url + "/jmxrmi");
                    Map<String, String[]> environment = new HashMap<String, String[]>();
                    if (username != null && pwd != null) {
                        // 用户名密码，在jmxremote.password文件中的密码
                        String[] credentials = new String[]{username, pwd};
                        environment.put("jmx.remote.credentials", credentials);
                    }

                    jmxConnector = JMXConnectorFactory.connect(ServiceURL, environment);

                    MBeanServerConnection mbsc = jmxConnector.getMBeanServerConnection();

                    ObjectName threadpoolObjName = new ObjectName("Catalina:type=ThreadPool,*");

                    Set<ObjectName> s2 = mbsc.queryNames(threadpoolObjName, null);
                    for (ObjectName obj : s2) {
                        if (!obj.getKeyProperty("name").contains(httpPort)) {
                            continue;
                        }
                        ObjectName objname = new ObjectName(obj.getCanonicalName());
                        logger.info("端口名:" + obj.getKeyProperty("name"));
                        int maxThreads = (Integer) mbsc.getAttribute(objname, "maxThreads");
                        logger.info("最大线程数:{}", maxThreads);
                        int currentThreadCount = (Integer) mbsc.getAttribute(objname, "currentThreadCount");
                        logger.info("当前线程数:{}", currentThreadCount);
                        int currentThreadsBusy = (Integer) mbsc.getAttribute(objname, "currentThreadsBusy");
                        logger.info("繁忙线程数:{}", currentThreadsBusy);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("ThreadMonitor run fail", e);
                } finally {
                    try {
                        if (jmxConnector != null) {
                            jmxConnector.close();
                        }
                    } catch (Exception e) {
                        logger.error("jmxConnector close fail", e);
                    }
                }

            }
        }, delay, period);
    }

}