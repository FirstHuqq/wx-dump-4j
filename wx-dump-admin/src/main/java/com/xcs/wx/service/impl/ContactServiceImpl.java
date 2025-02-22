package com.xcs.wx.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.xcs.wx.domain.dto.ContactDTO;
import com.xcs.wx.domain.vo.ContactLabelVO;
import com.xcs.wx.domain.vo.ContactVO;
import com.xcs.wx.domain.vo.ExportContactVO;
import com.xcs.wx.domain.vo.PageVO;
import com.xcs.wx.mapping.ContactLabelMapping;
import com.xcs.wx.repository.ContactLabelRepository;
import com.xcs.wx.repository.ContactRepository;
import com.xcs.wx.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 联系人服务实现类
 *
 * @author xcs
 * @date 2023年12月22日 14时42分
 **/
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final ContactLabelRepository contactLabelRepository;
    private final ContactLabelMapping contactLabelMapping;

    @Override
    public PageVO<ContactVO> queryContact(ContactDTO contactDTO) {
        // 分页查询联系人
        return Optional.ofNullable(contactRepository.queryContact(contactDTO))
                .map(page -> {
                    Map<String, String> contactLabelMap = contactLabelRepository.queryContactLabelAsMap();
                    for (ContactVO contactVO : page.getRecords()) {
                        // 分割当前联系人标签
                        List<String> labels = Arrays.stream(contactVO.getLabelIdList().split(","))
                                .map(contactLabelMap::get)
                                .filter(StrUtil::isNotBlank)
                                .collect(Collectors.toList());
                        // 设置标签
                        contactVO.setLabels(labels);
                    }
                    // 返回分页数据
                    return new PageVO<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
                })
                // 默认值
                .orElse(new PageVO<>(contactDTO.getCurrent(), contactDTO.getPageSize(), 0L, null));
    }

    @Override
    public List<ContactLabelVO> queryContactLabel() {
        // 查询标签
        return Optional.ofNullable(contactLabelRepository.queryContactLabelAsList())
                // 转换参数
                .map(contactLabelMapping::convert)
                // 设置默认值
                .orElse(Collections.emptyList());
    }

    @Override
    public String exportContact() {
        // 分隔符
        String separator = System.getProperty("file.separator");
        // 文件路径
        String filePath = System.getProperty("user.dir") + separator + "export";
        // 创建文件
        FileUtil.mkdir(filePath);
        // 文件路径+文件名
        String pathName = filePath + separator + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss") + "联系人" + ".xlsx";
        // 导出
        EasyExcel.write(pathName, ExportContactVO.class)
                .sheet("sheet1")
                .doWrite(contactRepository::exportContact);
        // 返回写入后的文件
        return pathName;
    }
}
