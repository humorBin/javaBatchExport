package com.zkhc.mrc.modular.system.controller;

import cn.stylefeng.roses.core.base.controller.BaseController;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zkhc.mrc.core.beetl.ShiroExt;
import com.zkhc.mrc.core.common.constant.Const;
import com.zkhc.mrc.core.shiro.ShiroKit;
import com.zkhc.mrc.core.shiro.ShiroUser;
import com.zkhc.mrc.modular.system.model.Dept;
import com.zkhc.mrc.modular.system.service.IDeptService;
import com.zkhc.mrc.modular.system.transfer.ReportSearchDto;
import org.apache.shiro.subject.Subject;
import org.beetl.core.misc.ALU;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.beans.factory.annotation.Autowired;
import com.zkhc.mrc.core.log.LogObjectHolder;
import org.springframework.web.bind.annotation.RequestParam;
import com.zkhc.mrc.modular.system.model.Report;
import com.zkhc.mrc.modular.system.service.IReportService;

import javax.servlet.http.HttpServletResponse;
import javax.swing.plaf.PanelUI;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Controller
@RequestMapping("/report")
public class ReportController extends BaseController {

    /**
     * 批量导出：
	 * 前段传入多个ID：23,25,2,4235,234，
	 * 根据ID查询数据库，拿到多个URL 遍历访问，获取多个数据流，并写出到ZIPOutputStream，response给浏览器。
     */
    @RequestMapping(value = "/batchExport")
    @ResponseBody
    public void batchExport(String reportArr, HttpServletResponse response){
		List<Report> reportList = reportService.selectExcelList(reportArr);

		response.setContentType("application/octet-stream");//通知浏览器 接受数据的格式。
		response.setHeader("Content-Disposition", "attachment; filename=report_data.zip");//压缩包的名字
		ZipOutputStream zos = null;
		InputStream leftInputStream = null;
		InputStream rightInputStream = null;

		try {
			zos = new ZipOutputStream(response.getOutputStream());
			HashMap<String, String[]> reportMap = new LinkedHashMap<>();
			HashMap<String, Integer> flagMap = new HashMap<String, Integer>();

			//处理查询的数据并封装 为Map
			//Map格式 <李四_2019-03-20 15:08:13_1, {muxiaoqian/8384ce45ee90433db0b9d3952c0d209e.dat, muxiaoqian/440e0ad2053244868d354e54dc2f3e34.dat}>
			for ( int i=1; i<= reportList.size(); i++) {
				Report r = reportList.get(i-1);
				String [] left_right_dataName = {r.getLeftDataUrl(), r.getRightDataUrl()};
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String folderName = r.getPatientName() + "_" + sdf.format(r.getReportTime());

				//处理同名文件
				if(flagMap.containsKey(folderName)){
					Integer value = flagMap.get(folderName) + 1;
					flagMap.put(folderName, value);
					folderName += "_" + value;
				} else {
					flagMap.put(folderName,0);
				}
				reportMap.put(folderName, left_right_dataName);
			}

			//导出压缩包
			String httpURL = Const.ALIYUN_OSS_URL;
			for (Map.Entry<String, String[]> entry: reportMap.entrySet() ) {
				String folderName =  entry.getKey() +"/";
				String left_data_url = entry.getValue()[0];
				String right_data_url = entry.getValue()[1];

				zos.putNextEntry(new ZipEntry(folderName));
				zos.putNextEntry(new ZipEntry(folderName + left_data_url.split("/")[1].replace(".dat","-left.dat")));
				leftInputStream = new URL(httpURL + left_data_url).openStream();
				doCompress(zos, leftInputStream);

				zos.putNextEntry(new ZipEntry(folderName + right_data_url.split("/")[1].replace(".dat","-right.dat")));
				rightInputStream = new URL(httpURL + right_data_url).openStream();
				doCompress(zos, rightInputStream);
			}
			zos.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			finishAll(zos, leftInputStream, rightInputStream);
		}
    }


	/**
	 * 压缩数据流到输出流
	 * @param zos
	 * @param inputStream
	 * @throws IOException
	 */
	private void doCompress(ZipOutputStream zos, InputStream inputStream) throws IOException {
		byte[] buffer = new byte[1024];
		int length;
		while ((length = inputStream.read(buffer)) > 0) {
			zos.write(buffer, 0, length);
		}
		inputStream.close();
		zos.closeEntry();
	}


	/**
	 * 关闭流
	 * @param zos
	 * @param leftInputStream
	 */
	private void finishAll(ZipOutputStream zos, InputStream leftInputStream, InputStream rightInputStream) {
		try {
			if(zos != null ){
				zos.flush();
				zos.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			if(leftInputStream != null ){
				leftInputStream.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			if(rightInputStream != null ){
				rightInputStream.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
