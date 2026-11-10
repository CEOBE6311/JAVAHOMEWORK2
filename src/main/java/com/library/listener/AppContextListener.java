package com.library.listener;

import com.library.util.DbConfig;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.logging.Logger;

@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOG.info("LibraryWeb2026 启动，数据库: " + DbConfig.url());
        LOG.info("验证码开关: " + DbConfig.captchaEnabled());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOG.info("LibraryWeb2026 停止");
    }
}
