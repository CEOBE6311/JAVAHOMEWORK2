package com.library.servlet;

import javax.imageio.ImageIO;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.SecureRandom;

/** 4 位图片验证码。 */
@WebServlet("/api/captcha")
public class CaptchaServlet extends HttpServlet {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int width = 120;
        int height = 40;
        StringBuilder code = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            code.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }

        HttpSession session = request.getSession(true);
        session.setAttribute("captcha", code.toString());
        session.setAttribute("captchaTime", System.currentTimeMillis());

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(245, 247, 250));
            g.fillRect(0, 0, width, height);

            for (int i = 0; i < 8; i++) {
                g.setColor(randomColor(150, 220));
                g.drawLine(RANDOM.nextInt(width), RANDOM.nextInt(height),
                        RANDOM.nextInt(width), RANDOM.nextInt(height));
            }

            g.setFont(new Font("Arial", Font.BOLD, 26));
            for (int i = 0; i < code.length(); i++) {
                g.setColor(randomColor(30, 130));
                double angle = (RANDOM.nextDouble() - 0.5) * 0.35;
                int x = 14 + i * 26;
                int y = 29 + RANDOM.nextInt(5) - 2;
                g.rotate(angle, x + 8, y - 8);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
                g.rotate(-angle, x + 8, y - 8);
            }
        } finally {
            g.dispose();
        }

        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        ImageIO.write(image, "png", response.getOutputStream());
    }

    private static Color randomColor(int min, int max) {
        int range = max - min;
        return new Color(min + RANDOM.nextInt(range), min + RANDOM.nextInt(range), min + RANDOM.nextInt(range));
    }
}
