package com.tandian.system.service;

import com.tandian.system.entity.ScheduledTask;
import com.tandian.system.vo.ShopVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 邮件发送服务
 * 从全局系统配置读取SMTP信息
 */
@Slf4j
@Service
public class EmailService {

    @Resource
    private SystemConfigService systemConfigService;

    /**
     * 从全局配置创建JavaMailSender
     */
    public JavaMailSender createMailSender() {
        Map<String, String> config = systemConfigService.getMailConfig();

        String host = config.getOrDefault("smtp_host", "smtp.163.com");
        int port = Integer.parseInt(config.getOrDefault("smtp_port", "465"));
        String username = config.getOrDefault("smtp_username", "");
        String password = config.getOrDefault("smtp_password", "");
        boolean useSsl = Boolean.parseBoolean(config.getOrDefault("smtp_ssl", "true"));

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.trust", host);

        if (useSsl) {
            // 465端口 SSL直连
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", String.valueOf(port));
        } else {
            // 587端口 STARTTLS
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        return sender;
    }

    /**
     * 发送HTML邮件
     */
    public void sendHtmlEmail(ScheduledTask task, String subject, String htmlContent) {
        if (task.getNotifyEnabled() == null || task.getNotifyEnabled() != 1) {
            log.info("任务 {} 未开启通知，跳过邮件发送", task.getTaskName());
            return;
        }

        if (task.getEmails() == null || task.getEmails().trim().isEmpty()) {
            log.warn("任务 {} 未配置通知邮箱，跳过邮件发送", task.getTaskName());
            return;
        }

        Map<String, String> config = systemConfigService.getMailConfig();
        String username = config.getOrDefault("smtp_username", "");
        String password = config.getOrDefault("smtp_password", "");

        if (username.isEmpty() || password.isEmpty()) {
            log.warn("邮件服务器未配置账号密码，跳过邮件发送");
            return;
        }

        try {
            JavaMailSender sender = createMailSender();
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(username);
            String[] emailArray = task.getEmails().split(",");
            helper.setTo(emailArray);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            sender.send(message);
            log.info("邮件发送成功：{} -> {}", subject, task.getEmails());
        } catch (Exception e) {
            log.error("邮件发送失败：{}", e.getMessage(), e);
            throw new RuntimeException("邮件发送失败：" + e.getMessage());
        }
    }

    /**
     * 生成即将过期提醒邮件HTML
     */
    public String buildExpireReminderHtml(List<ShopVO> shops, String expireDate) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto;'>");
        sb.append("<h2 style='color: #ff6b9d;'>探店即将过期提醒</h2>");
        sb.append("<p style='color: #666; font-size: 14px;'>以下店铺将于 <b style='color: #ff4d4f;'>").append(expireDate).append("</b> 过期，请尽快安排探店！</p>");
        sb.append("<table style='width: 100%; border-collapse: collapse; margin-top: 16px;'>");
        sb.append("<tr style='background: #ff6b9d; color: white;'>");
        sb.append("<th style='padding: 10px; border: 1px solid #eee; text-align: left;'>店铺名称</th>");
        sb.append("<th style='padding: 10px; border: 1px solid #eee; text-align: left;'>类别</th>");
        sb.append("<th style='padding: 10px; border: 1px solid #eee; text-align: left;'>地址</th>");
        sb.append("<th style='padding: 10px; border: 1px solid #eee; text-align: left;'>可用人数</th>");
        sb.append("<th style='padding: 10px; border: 1px solid #eee; text-align: left;'>过期时间</th>");
        sb.append("</tr>");

        for (int i = 0; i < shops.size(); i++) {
            ShopVO shop = shops.get(i);
            String bgColor = i % 2 == 0 ? "#fff5f7" : "#ffffff";
            sb.append("<tr style='background: ").append(bgColor).append(";'>");
            sb.append("<td style='padding: 8px; border: 1px solid #eee;'>").append(shop.getName()).append("</td>");
            sb.append("<td style='padding: 8px; border: 1px solid #eee;'>").append(shop.getCategoryText() != null ? shop.getCategoryText() : "其他").append("</td>");
            sb.append("<td style='padding: 8px; border: 1px solid #eee;'>").append(shop.getAddress()).append("</td>");
            sb.append("<td style='padding: 8px; border: 1px solid #eee;'>").append(shop.getAvailableCount() != null ? shop.getAvailableCount() : 1).append(" 人</td>");
            sb.append("<td style='padding: 8px; border: 1px solid #eee; color: #ff4d4f; font-weight: bold;'>").append(shop.getExpireTime()).append("</td>");
            sb.append("</tr>");
        }

        sb.append("</table>");
        sb.append("<p style='color: #999; font-size: 12px; margin-top: 20px;'>此邮件由「大众点评探店路线规划系统」自动发送，请勿回复。</p>");
        sb.append("</div>");
        return sb.toString();
    }

    /**
     * 生成每周推荐邮件HTML
     */
    public String buildWeeklyRecommendHtml(List<ShopVO> shops) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto;'>");
        sb.append("<h2 style='color: #ff6b9d;'>探店每周推荐</h2>");
        sb.append("<p style='color: #666; font-size: 14px;'>本周为您精选了 <b>").append(shops.size()).append("</b> 家最值得探店的店铺，快来看看吧！</p>");
        sb.append("<table style='width: 100%; border-collapse: collapse; margin-top: 16px;'>");
        sb.append("<tr style='background: #ff6b9d; color: white;'>");
        sb.append("<th style='padding: 10px; border: 1px solid #eee; text-align: left;'>排名</th>");
        sb.append("<th style='padding: 10px; border: 1px solid #eee; text-align: left;'>店铺名称</th>");
        sb.append("<th style='padding: 10px; border: 1px solid #eee; text-align: left;'>类别</th>");
        sb.append("<th style='padding: 10px; border: 1px solid #eee; text-align: left;'>地址</th>");
        sb.append("<th style='padding: 10px; border: 1px solid #eee; text-align: left;'>可用人数</th>");
        sb.append("<th style='padding: 10px; border: 1px solid #eee; text-align: left;'>过期时间</th>");
        sb.append("</tr>");

        for (int i = 0; i < shops.size(); i++) {
            ShopVO shop = shops.get(i);
            String bgColor = i % 2 == 0 ? "#fff5f7" : "#ffffff";
            sb.append("<tr style='background: ").append(bgColor).append(";'>");
            sb.append("<td style='padding: 8px; border: 1px solid #eee; font-weight: bold; color: #ff6b9d;'>").append(i + 1).append("</td>");
            sb.append("<td style='padding: 8px; border: 1px solid #eee;'>").append(shop.getName()).append("</td>");
            sb.append("<td style='padding: 8px; border: 1px solid #eee;'>").append(shop.getCategoryText() != null ? shop.getCategoryText() : "其他").append("</td>");
            sb.append("<td style='padding: 8px; border: 1px solid #eee;'>").append(shop.getAddress()).append("</td>");
            sb.append("<td style='padding: 8px; border: 1px solid #eee;'>").append(shop.getAvailableCount() != null ? shop.getAvailableCount() : 1).append(" 人</td>");
            sb.append("<td style='padding: 8px; border: 1px solid #eee;'>").append(shop.getExpireTime() != null ? shop.getExpireTime().toString() : "不限").append("</td>");
            sb.append("</tr>");
        }

        sb.append("</table>");
        sb.append("<p style='color: #999; font-size: 12px; margin-top: 20px;'>此邮件由「大众点评探店路线规划系统」自动发送，请勿回复。</p>");
        sb.append("</div>");
        return sb.toString();
    }
}
