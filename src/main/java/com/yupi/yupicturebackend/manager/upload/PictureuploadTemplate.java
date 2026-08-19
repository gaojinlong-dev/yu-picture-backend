package com.yupi.yupicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.yupi.yupicturebackend.config.CosClientConfig;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.manager.CosManager;
import com.yupi.yupicturebackend.model.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * 文件上传模板
 */
@Slf4j
public abstract class PictureuploadTemplate {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     *
     * @param inputSoure       文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 上传结果
     */
    public UploadPictureResult uploadPicture(Object inputSoure, String uploadPathPrefix) {

        //1,校验 文件
        validPicture(inputSoure);

        //2, 图片上传的地址，前缀+ 文件名
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginalFilename(inputSoure);
        //自己拼接上传文件路径而不是原始文件路径，增加安全性
        String upLoadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()),
                uuid, originalFilename);
        String uploadPath = String.format("%s/%s", uploadPathPrefix, upLoadFilename);

        File file = null;
        try {
            //3,创建临时文件,获取文件到服务器
            file = File.createTempFile(uploadPath, null);
            // 处理文件来源
            processFile(inputSoure, file);
            //4，上传图片到服务器
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 5，获取图片信息对象，返回分装结果
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 获取到图片处理结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)){
                CIObject compressedCiObject = objectList.get(0);
                //缩略图默认等于压缩图
                CIObject thumbnailCiobject = compressedCiObject;
                //有生成缩略图才获取缩略图
                if (objectList.size()> 1){
                    // 封装压缩缩略的返回结果
                     thumbnailCiobject = objectList.get(1);
                }

                // 封装压缩图的返回结果
                return bulidResult(originalFilename,compressedCiObject,thumbnailCiobject);
            }
            return bulidResult(originalFilename, file, uploadPath, imageInfo);

        } catch (Exception e) {
            log.error("上传图片失败", e);

            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 6，删除临时文件
            this.deleteFile(file);
        }
    }

    /**
     * 封装返回结果
     * @param originalFilename 原始文件名
     * @param compressedCiObject 压缩后的对象
     * @param thumbnailCiObject  缩略图对象
     * @return
     */
    private UploadPictureResult bulidResult(String originalFilename, CIObject compressedCiObject,CIObject thumbnailCiObject) {
        // 计算图片宽高比
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        // 封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        //设置压缩后的原图地址
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize((compressedCiObject.getSize().longValue()));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        //设置缩略图地址
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/"  + thumbnailCiObject.getKey());
        // 返回结果
        return uploadPictureResult;
    }


    /**
     * 处理文件源并生成本地临时文件
     *
     * @param inputSoure
     */
    protected abstract void processFile(Object inputSoure, File file) throws Exception;

    /**
     * 获取输入源的原始文件名
     *
     * @param inputSoure
     * @return
     */

    protected abstract String getOriginalFilename(Object inputSoure);

    /**
     * 校验输入源，(本地文件，或url)
     *
     * @param inputSoure
     */

    protected abstract void validPicture(Object inputSoure);


    /**
     * 构建上传结果
     *
     * @param imageInfo
     * @param uploadPath
     * @param originalFilename
     * @param file
     * @return uploadPictureResult 对象返回的图片信息
     */
    private UploadPictureResult bulidResult(String originalFilename, File file, String uploadPath, ImageInfo imageInfo) {
        // 计算图片宽高比
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        // 封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        // 返回结果
        return uploadPictureResult;
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

//
//    /**
//     * 校验图片
//     *
//     * @param fileUrl
//     */
//    private void validPicture(String fileUrl) {
//        // 1，校验非空
//        ThrowUtils.throwIf(fileUrl == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
//        // 2，校验 url格式
//        try {
//            new URL(fileUrl);
//        } catch (MalformedURLException e) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
//        }
//        //3， 校验url的协议
//        ThrowUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"), ErrorCode.PARAMS_ERROR, "仅支持HTTP或HTTPS协议的文件地址");
//
//        // 4，发送HADE请求，验证文件是否存在
//        HttpResponse httpResponse = null;
//        try {
//            httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
//            // 未正常返回,无需执行其他判断，有些不会有HADE请求
//            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
//                return;
//            }
//            // 5，文件存在，文件类型校验
//            String contentType = httpResponse.header("Content-Type");
//            // 6，不为空，才校验是否合法，否则不校验
//            if (contentType != null) {
//                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/png", "image/jng",
//                        "image/webp");
//                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType), ErrorCode.PARAMS_ERROR, "文件类型错误");
//            }
//            // 7，文件存在，文件大小校验
//            String contentLengthStr = httpResponse.header("Content-Length");
//            if (StrUtil.isNotBlank(contentLengthStr)) {
//                try {
//                    long contentLength = Long.parseLong(contentLengthStr);
//                    final long ONE_M = 1024 * 1024;
//                    ThrowUtils.throwIf(contentLength > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过2M");
//
//                } catch (NumberFormatException e) {
//                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式异常");
//                }
//            }
//        } finally {
//            // 8，释放资源
//            if (httpResponse != null) {
//                httpResponse.close();
//            }
//        }
//
//
//    }


}
