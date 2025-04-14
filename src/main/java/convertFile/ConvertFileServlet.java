package convertFile;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

@WebServlet("/convertFile")
public class ConvertFileServlet extends HttpServlet {

    private static final String UPLOAD_DIRECTORY = "uploads"; // Temporary directory for storing uploaded files

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Check if the request is multipart (file upload)
        if (ServletFileUpload.isMultipartContent(request)) {
            // Set up the file upload handler
            DiskFileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);

            // Create the upload directory if it doesn't exist
            File uploadDir = new File(getServletContext().getRealPath("") + File.separator + UPLOAD_DIRECTORY);
            if (!uploadDir.exists()) uploadDir.mkdir();

            try {
                // Parse the request to get uploaded items
                List<FileItem> formItems = upload.parseRequest(request);
                String outputFormat = ""; // Desired output format (pdf, txt, etc.)
                File uploadedFile = null;

                // Process uploaded form fields
                for (FileItem item : formItems) {
                    if (!item.isFormField()) {
                        String fileName = new File(item.getName()).getName();
                        String filePath = uploadDir + File.separator + fileName;
                        uploadedFile = new File(filePath);
                        item.write(uploadedFile);
                    } else if (item.getFieldName().equals("outputFormat")) {
                        outputFormat = item.getString();
                    }
                }

                if (uploadedFile != null && !outputFormat.isEmpty()) {
                    if (outputFormat.equals("txt")) {
                        convertPdfToText(uploadedFile, response);
                    } else if (outputFormat.equals("pdf")) {
                        handlePdfConversion(uploadedFile, response);
                    } else if (outputFormat.equals("excel")) {
                        convertCsvToExcel(uploadedFile, response);
                    } else {
                        response.getWriter().write("Unsupported conversion format.");
                    }
                }
            } catch (Exception e) {
                response.getWriter().write("File upload failed: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            response.getWriter().write("Request is not multipart.");
        }
    }

    // Convert PDF to Text
    private void convertPdfToText(File inputFile, HttpServletResponse response) throws IOException {
        if (inputFile.getName().endsWith(".pdf")) {
            PDDocument document = PDDocument.load(inputFile);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            response.setContentType("text/plain");
            response.getWriter().write(text);
            document.close();
        } else {
            response.getWriter().write("Input file is not a PDF.");
        }
    }

    // Handle different types of PDF conversion
    private void handlePdfConversion(File uploadedFile, HttpServletResponse response) throws IOException {
        if (uploadedFile.getName().endsWith(".txt")) {
            convertTextToPdf(uploadedFile, response);
        } else if (uploadedFile.getName().endsWith(".jpg") || uploadedFile.getName().endsWith(".png")) {
            convertImageToPdf(uploadedFile, response);
        } else {
            response.getWriter().write("Unsupported file type for PDF conversion.");
        }
    }

    // Convert Text to PDF
    private void convertTextToPdf(File inputFile, HttpServletResponse response) throws IOException {
        if (inputFile.getName().endsWith(".txt")) {
            PDDocument document = new PDDocument();
            PDPage page = new PDPage();
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
            contentStream.newLineAtOffset(50, 750);
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            String line;
            int yPosition = 750;
            while ((line = reader.readLine()) != null) {
                if (yPosition < 50) { // Start new page if content exceeds current page
                    page = new PDPage();
                    document.addPage(page);
                    contentStream.close();
                    contentStream = new PDPageContentStream(document, page);
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    contentStream.newLineAtOffset(50, 750);
                    yPosition = 750;
                }
                contentStream.showText(line);
                yPosition -= 15;
                contentStream.newLineAtOffset(0, -15);
            }
            reader.close();
            contentStream.endText();
            contentStream.close();
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=" + inputFile.getName().replace(".txt", ".pdf"));
            document.save(response.getOutputStream());
            document.close();
        } else {
            response.getWriter().write("Input file is not a text file.");
        }
    }

    // Convert Image to PDF (JPEG/PNG)
    private void convertImageToPdf(File inputFile, HttpServletResponse response) throws IOException {
        if (inputFile.getName().endsWith(".jpg") || inputFile.getName().endsWith(".png")) {
            PDDocument document = new PDDocument();
            PDPage page = new PDPage();
            document.addPage(page);

            // Get the image and its dimensions
            PDImageXObject pdImage = PDImageXObject.createFromFile(inputFile.getAbsolutePath(), document);
            float imageWidth = pdImage.getWidth();
            float imageHeight = pdImage.getHeight();

            // Get the page size
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();

            // Calculate scaling factor to fit image into the page
            float scaleX = pageWidth / imageWidth;
            float scaleY = pageHeight / imageHeight;
            float scale = Math.min(scaleX, scaleY); // Use the smaller scale to ensure the image fits

            // Calculate the position to center the image on the page
            float x = (pageWidth - imageWidth * scale) / 2;
            float y = (pageHeight - imageHeight * scale) / 2;

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.drawImage(pdImage, x, y, imageWidth * scale, imageHeight * scale);
            contentStream.close();

            // Set the response type and header for the PDF download
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=" + inputFile.getName().replace(".jpg", ".pdf").replace(".png", ".pdf"));

            // Write the PDF to the response output stream
            document.save(response.getOutputStream());
            document.close();
        } else {
            response.getWriter().write("Input file is not an image.");
        }
    }


    // Convert CSV to Excel
    private void convertCsvToExcel(File inputFile, HttpServletResponse response) throws IOException {
        if (inputFile.getName().endsWith(".csv")) {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Sheet1");
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            String line;
            int rowNum = 0;
            while ((line = br.readLine()) != null) {
                String[] columns = line.split(",");
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < columns.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(columns[i]);
                }
            }
            br.close();
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment; filename=" + inputFile.getName().replace(".csv", ".xlsx"));
            workbook.write(response.getOutputStream());
            workbook.close();
        } else {
            response.getWriter().write("Input file is not a CSV.");
        }
    }
}
