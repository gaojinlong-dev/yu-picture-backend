package com.yupi.yupicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.yupi.yupicturebackend.config.CosClientConfig;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     *
     * @param multipartFile    文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 上传结果
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {

        //校验 文件
        validPicture(multipartFile);

        // 图片上传的地址，前缀+ 文件名
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        //自己拼接上传文件路径而不是原始文件路径，增加安全性
        String upLoadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()),
                uuid, originalFilename);
        String uploadPath = String.format("%s/%s", uploadPathPrefix, upLoadFilename);

        File file = null;
        try {
            //创建临时文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            //上传文件
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 封装返回结果
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);

            return uploadPictureResult;

        } catch (Exception e) {
            log.error("上传图片失败", e);

            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 删除临时文件
            this.deleteFile(file);
        }
    }

    /**
     * 删除文件
     * 文件上传时会在本地生成临时文件，无论是否上传成功，都要删除文件，否则到导致资源泄露，
     *
     * @param file
     */
    private void deleteFile(File file) {
        if (file == null) {
            return;
        }
        // 删除临时文件
        boolean result = file.delete();
        if (!result) {
            log.error("file delete error,filepathP{}", file.getAbsoluteFile());
        }

    }

    /**
     * 校验图片
     * 文件的校验规则复杂可以抽出为一个的方法，对文件的大小，格式，类型或者内容进行校验
     *
     * @param multipartFile
     */
    private void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 1，校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过2M");
        // 2，校验文件后缀
        String fileSuffux = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        //定义允许上传的图片格式
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffux), ErrorCode.PARAMS_ERROR, "文件格式错误");


    }


    // todo 新增的方法

    /**
     * 通过url上传文件
     *
     * @param fileUrl          上传文件 url
     * @param uploadPathPrefix 上传路径前缀
     * @return 上传结果
     */
    public UploadPictureResult uploadPictureByUrl(String fileUrl, String uploadPathPrefix) {

        //校验 文件
//        validPicture(multipartFile);
        // todo
        validPicture(fileUrl);


        // 图片上传的地址，前缀+ 文件名
        String uuid = RandomUtil.randomString(16);
//        String originalFilename = multipartFile.getOriginalFilename();
        // todo  https://www.baidu.com/img/PCtm_d9c8750bed0b3c7d089fa7d55720d6cf.png
        String originalFilename = FileUtil.mainName(fileUrl);
        //自己拼接上传文件路径而不是原始文件路径，增加安全性
        String upLoadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()),
                uuid, originalFilename);
        String uploadPath = String.format("%s/%s", uploadPathPrefix, upLoadFilename);

        File file = null;
        try {
            //创建临时文件
            file = File.createTempFile(uploadPath, null);
            // 下载文件
            // todo
            HttpUtil.downloadFile(fileUrl, file);
//            multipartFile.transferTo(file);
            //上传文件
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 封装返回结果
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);

            return uploadPictureResult;

        } catch (Exception e) {
            log.error("上传图片失败", e);

            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 删除临时文件
            this.deleteFile(file);
        }
    }

    /**
     * 校验图片
     *
     * @param fileUrl
     */
    private void validPicture(String fileUrl) {
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
