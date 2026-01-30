package top.enderliquid.audioflow.common.config;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

@ControllerAdvice
public class GlobalBindingAdvice {
    // 自动 Trim 包含文件的表单 (multipart/form-data) 和 URL 表单 (application/x-www-form-urlencoded) 传入的字符串
    // 字符串为空则设为 null
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        StringTrimmerEditor stringTrimmerEditor = new StringTrimmerEditor(true);
        binder.registerCustomEditor(String.class, stringTrimmerEditor);
    }
}
