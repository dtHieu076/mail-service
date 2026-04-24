package org.atlas_erp;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class GenerateRecipients {

    public static void main(String[] args) {
        int start = 1;
        int end = 1000; // bạn có thể đổi lên 1000 nếu muốn test load lớn

        String outputFile = "recipients.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            for (int i = start; i <= end; i++) {
                String line = String.format(
                        "{\"email\": \"user%d@atlas-erp.com\", \"name\": \"User %d\"}%s",
                        i,
                        i,
                        (i < end ? "," : "") // thêm dấu , trừ dòng cuối
                );

                writer.write(line);
                writer.newLine();
            }

            System.out.println("✅ Đã generate xong file: " + outputFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}