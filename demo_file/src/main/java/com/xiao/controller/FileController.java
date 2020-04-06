package com.xiao.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.jodconverter.core.DocumentConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FileController {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileController.class); 

	@RequestMapping("/fileTest")
	public String fileTest() {
		return "fileTest";
	}
	
	/**
	 * 单文件上传
	 * @param file
	 * @return
	 * @throws IOException 
	 */
    @PostMapping("/uploadFile")
    @ResponseBody
    public String uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return "上传失败，请选择文件";
        }

        String fileName = file.getOriginalFilename();
        System.out.println("originalFilename==" + fileName); // 文件名.文档类型
        System.out.println("name==" + file.getName());// 获取的是input中 name 的值
        System.out.println("size==" + file.getSize());// 文件大小，单位：B
        
        String filePath = "E:/temptest/";
        File dest = new File(filePath + fileName);
        try {
        	// 方式一
            file.transferTo(dest);
            // 方式二
            //IOUtils.copy(file.getInputStream(), new FileOutputStream(dest));
            LOGGER.info("上传成功");
            return "上传成功";
        } catch (IOException e) {
            LOGGER.error(e.toString(), e);
        }
        return "上传失败！";
    }
    
    /**
     * 多文件上传
     * @param file
     * @return
     */
    @PostMapping("/uploadFiles")
    @ResponseBody
    public String uploadFiles(HttpServletRequest request) {
    	List<MultipartFile> files = ((MultipartHttpServletRequest) request).getFiles("file");
        String filePath = "E:/temptest/";
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            if (file.isEmpty()) {
            	System.out.println("未找到附件。。。");
                System.out.println("上传第" + (++i) + "个文件失败");
                i--; // 需要判断下一个附件是否可以进行上传，把上一行+1的下标减回去
                continue;
            }
            String fileName = file.getOriginalFilename();

            File dest = new File(filePath + fileName);
            try {
                file.transferTo(dest);
                LOGGER.info("第" + (i + 1) + "个文件上传成功");
            } catch (IOException e) {
                LOGGER.error(e.toString(), e);
                
                return "上传第" + (++i) + "个文件失败";
            }
        }

        return "上传成功";
    }
    
    /**
     * 上传到项目的某个目录（该方法还未具体查看、具体测试）
     * @param srcFile
     * @param redirectAttributes
     * @return
     */
    @PostMapping("/upload")
    public String fileUpload(@RequestParam("file")MultipartFile srcFile, RedirectAttributes redirectAttributes) {
	    //前端没有选择文件，srcFile为空
	    if(srcFile.isEmpty()) {
		    redirectAttributes.addFlashAttribute("message", "请选择一个文件");
		    return null;
	    }
	    //选择了文件，开始上传操作
	    try {
		    //构建上传目标路径，找到了项目的target的classes目录
		    File destFile = new File(ResourceUtils.getURL("classpath:").getPath());
		    if(!destFile.exists()) {
		    	destFile = new File("");
		    }
		    //输出目标文件的绝对路径
		    System.out.println("file path:"+destFile.getAbsolutePath());
		    //拼接子路径
		    SimpleDateFormat sf_ = new SimpleDateFormat("yyyyMMddHHmmss");
		    String times = sf_.format(new Date());
		    File upload = new File(destFile.getAbsolutePath(), "picture/"+times);
		    //若目标文件夹不存在，则创建
		    if(!upload.exists()) {
		    	upload.mkdirs();
		    }
		    System.out.println("完整的上传路径："+upload.getAbsolutePath()+"/"+srcFile);
		    //根据srcFile大小，准备一个字节数组
		    byte[] bytes = srcFile.getBytes();
		    //拼接上传路径
		    //Path path = Paths.get(UPLOAD_FOLDER + srcFile.getOriginalFilename());
		    //通过项目路径，拼接上传路径
		    Path path = Paths.get(upload.getAbsolutePath()+"/"+srcFile.getOriginalFilename());
		    //** 开始将源文件写入目标地址
		    Files.write(path, bytes);
		    String uuid = UUID.randomUUID().toString().replaceAll("-", "");
		    // 获得文件原始名称
		    String fileName = srcFile.getOriginalFilename();
		    // 获得文件后缀名称
		    String suffixName = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
		    // 生成最新的uuid文件名称
		    String newFileName = uuid + "."+ suffixName;
		    redirectAttributes.addFlashAttribute("message", "文件上传成功"+newFileName);
		} catch (IOException e) {
		    e.printStackTrace();
	    }
	    return "redirect:upload_status";
    }

	
	/**
	 * 文件下载
	 * .doc .docx .pdf .xls .xlsx .jpg .png .gif .txt .js .css .html .java的文件均可下载成功
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	@RequestMapping("/downFile")
	public ResponseEntity<FileSystemResource> down()throws UnsupportedEncodingException{
		String uploadPath = "E:/testFile";
		String realimgurl=uploadPath+"/DemoOneApplication.java";
		//String realimgurl=uploadPath+"/scDoc/Self-service analysis tool _ user manual.doc";
		System.out.println(realimgurl);
		
        File file = new File(realimgurl);
        if (file == null){
            return null;
        }
        
        String suffixType =realimgurl.substring(realimgurl.lastIndexOf("."));
        String newfilename ="自定义文件名称"+suffixType;
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Content-Disposition", "attachment; filename=" +  URLEncoder.encode(newfilename, "UTF-8"));
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
 
        return ResponseEntity
                .ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(new FileSystemResource(file));
		
	}
	
	/**
	 * pdf.js实现，只能预览pdf
	 * @param request
	 * @param response
	 */
	@RequestMapping("/preview")
	public void pdfPreview(HttpServletRequest request, HttpServletResponse response) {
        //PDF文件地址
		File file = new File("E:/testFile/temp.pdf");
		if (file.exists()) {
			byte[] data = null;
			FileInputStream input=null;
			try {
				input= new FileInputStream(file);
				data = new byte[input.available()];
				input.read(data);
				response.getOutputStream().write(data);
			} catch (Exception e) {
				System.out.println("pdf文件处理异常：" + e);
			}finally{
				try {
					if(input!=null){
						input.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	//--------------------------------------------------------------------------
	// 注入转换器
    @Autowired
    private DocumentConverter converter;

    @Autowired
    private HttpServletResponse response;

    @RequestMapping("/viewByPDF")
    public void viewByPDF() {
		File file = new File("E:/testFile/yue.jpg");//需要转换的文件
		try {
		    File newFile = new File("E:/prePDF");//转换之后文件生成的地址
		    if (!newFile.exists()) {
		    	newFile.mkdirs();
		    }
		    //文件转化
		    converter.convert(file).to(new File("E:/prePDF/view.pdf")).execute();
		    //使用response,将pdf文件以流的方式发送的前端
		    ServletOutputStream outputStream = response.getOutputStream();
		    InputStream in = new FileInputStream(new File("E:/prePDF/view.pdf"));// 读取文件
		    //InputStream in = new FileInputStream(new File("E:/testFile/temp.pdf"));// 读取文件
		    // copy文件
		    int i = IOUtils.copy(in, outputStream);
		    System.out.println(i);
		    in.close();
		    outputStream.close();
		} catch (Exception e) {
		    e.printStackTrace();
		}
		
    }
    
}
