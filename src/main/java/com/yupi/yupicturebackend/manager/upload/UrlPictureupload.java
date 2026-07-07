package com.yupi.yupicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * ClassName: UrlPictureupload
 * Package: com.yupi.yupicturebackend.manager.upload
 * Description: Url图片上传
 *
 * @Author gjl
 * @Create 2026/5/15 23:02
 * @Version 1.0
 */
@Service
public class UrlPictureupload extends PictureuploadTemplate {
    @Override
    protected void processFile(Object inputSoure, File file) throws Exception {
        String fileUrl = (String) inputSoure;
        HttpUtil.downloadFile(fileUrl, file);
    }

    @Override
    protected String getOriginalFilename(Object inputSoure) {
        String fileUrl = (String) inputSoure;
        return  FileUtil.mainName(fileUrl);
    }

    @Override
    protected void validPicture(Object inputSoure) {
        String fileUrl = (String) inputSoure;
        // 1，校验非空
        ThrowUtils.throwIf(fileUrl == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 2，校验 url格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }
        //3， 校验url的协议
        ThrowUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"), ErrorCode.PARAMS_ERROR, "仅支持HTTP或HTTPS协议的文件地址");

        // 4，发送HADE请求，验证文件是否存在
        HttpResponse httpResponse = null;
        try {
            httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            // 未正常返回,无需执行其他判断，有些不会有HADE请求
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            // 5，文件存在，文件类型校验
            String contentType = httpResponse.header("Content-Type");
            // 6，不为空，才校验是否合法，否则不校验
            if (contentType != null) {
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/png", "image/jng",
                        "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType), ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            // 7，文件存在，文件大小校验
            String contentLengthStr = httpResponse.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long ONE_M = 1024 * 1024;
                    ThrowUtils.throwIf(contentLength > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过2M");

                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式异常");
                }
            }
        } finally {
            // 8，释放资源
            if (httpResponse != null) {
                httpResponse.close();
            }
        }


    }
}
