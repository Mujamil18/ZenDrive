package convertFile;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.tika.Tika;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@WebServlet("/convertToAudio")
public class mp4Tomp3 extends HttpServlet {

    private static final String UPLOAD_DIRECTORY = "uploads"; // Temporary directory for storing uploaded files

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Check if the request is multipart (file upload)
        if (ServletFileUpload.isMultipartContent(request)) {
            // Set up the file upload handler
            DiskFileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);

            // Create the upload directory if it does not exist
            File uploadDir = new File(getServletContext().getRealPath("") + File.separator + UPLOAD_DIRECTORY);
            if (!uploadDir.exists()) uploadDir.mkdir();

            try {
                // Parse the request to get uploaded items
                List<FileItem> formItems = upload.parseRequest(request);
                for (FileItem item : formItems) {
                    if (!item.isFormField()) {
                        // Get the uploaded file's name and path
                        String fileName = new File(item.getName()).getName();
                        String filePath = uploadDir + File.separator + fileName;
                        File storeFile = new File(filePath);

                        // Save the uploaded file to the server
                        item.write(storeFile);

                        // Use Apache Tika to detect the file type
                        Tika tika = new Tika();
                        String fileType = tika.detect(storeFile);
                        System.out.println("Detected file type: " + fileType);

                        // Check if the uploaded file is an MP4
                        if (fileType.equals("video/mp4")) {
                            // Convert the MP4 file to MP3
                            String mp3FilePath = convertMp4ToMp3(storeFile);

                            // Send the MP3 file as a response
                            sendMp3Response(response, mp3FilePath);

                            // Clean up: delete the temporary files
                            storeFile.delete();
                            new File(mp3FilePath).delete();
                        } else {
                            response.getWriter().write("Invalid file type. Please upload an MP4 file.");
                        }
                    }
                }
            } catch (Exception e) {
                throw new ServletException("File upload failed", e);
            }
        } else {
            response.getWriter().write("Request is not multipart");
        }
    }

    // Method to convert MP4 to MP3 using FFmpeg (via ProcessBuilder)
    private String convertMp4ToMp3(File mp4File) throws IOException {
        // Define the path for the output MP3 file
        String mp3FilePath = mp4File.getAbsolutePath().replace(".mp4", ".mp3");

        // Build the FFmpeg command
        String ffmpegCommand = "ffmpeg -i " + mp4File.getAbsolutePath() + " -vn -ar 44100 -ac 2 -ab 192k -f mp3 " + mp3FilePath;

        // Execute the command using ProcessBuilder
        ProcessBuilder processBuilder = new ProcessBuilder(ffmpegCommand.split(" "));
        processBuilder.redirectErrorStream(true); // Combine stdout and stderr
        Process process = processBuilder.start();

        // Wait for the process to finish
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("FFmpeg command failed with exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("FFmpeg process interrupted", e);
        }

        return mp3FilePath;
    }

    // Method to send the MP3 file as a response
    private void sendMp3Response(HttpServletResponse response, String mp3FilePath) throws IOException {
        File mp3File = new File(mp3FilePath);

        // Set the response content type to MP3
        response.setContentType("audio/mpeg");
        response.setHeader("Content-Disposition", "attachment; filename=" + mp3File.getName());

        // Write the MP3 file to the response output stream
        try (FileInputStream fis = new FileInputStream(mp3File);
             OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}
