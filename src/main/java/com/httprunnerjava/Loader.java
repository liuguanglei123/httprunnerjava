package com.httprunnerjava;

import com.google.common.base.Strings;
import com.httprunnerjava.model.ProjectMeta;
import com.httprunnerjava.model.lazyLoading.LazyString;
import com.httprunnerjava.utils.CSVFileUtil;
import com.httprunnerjava.utils.ClassUtils;
import com.httprunnerjava.utils.CompilerFile;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
public class Loader {

    // 这里和hrun原版稍有差别，如果以后想支持多线程执行，那么线程变量必不可少，其实这里也是一种推测，目前也用不到
    public static final ThreadLocal<ProjectMeta> projectMetaContext = new ThreadLocal<>();

    public static void clear() {
        projectMetaContext.remove();
    }

    /**
     * 目前在仅支持class执行用例的情况下，test_path值一定为空
     * @param test_path 这个是原版带过来的参数，暂时用不到，可以认为其值一定为 null
    */
    public static ProjectMeta loadProjectMeta (LazyString test_path){

        if(test_path == null) {
            return loadProjectMeta(null, false);
        }
        // TODO：下面的逻辑暂时用不到
        return loadProjectMeta(test_path.getRawValue(),false);
    }

    /**
     *
     * @param testPackagePath
     * @param reload 这个是原版带过来的参数，暂时用不到，可以认为其值一定为 false
     * @return
     */
    public static ProjectMeta loadProjectMeta (String testPackagePath, boolean reload){
        if(HttpRunner.getProjectMeta() != null && !reload) {
            return HttpRunner.getProjectMeta();
        }

        ProjectMeta projectMeta = new ProjectMeta();
        if(!Strings.isNullOrEmpty(testPackagePath)) {
            projectMeta.setFunctions(ClassUtils.getDefaultDebugtalkClass(testPackagePath));
        }

        projectMeta.setEnvVar(loadEnvFile());
        return projectMeta;
    }

    public static Class<?> loadBuiltinFunctions() {
        return load_module_functions("builtin");
    }

    public static Class<?> load_module_functions(String module) {
        return CompilerFile.loadClass(module);
    }

    public static Class<?> loadLoadFileClass(){
        return CompilerFile.loadLoadFileClass();
    }

    public static List<Map<String,String>> loadCsvFile(String csvFile){
        String newCsvFilePath =  csvFile.replace("/","\\");
        //只支持加载resource目录下的文件
        InputStream inputStream = Loader.class.getClassLoader().getResourceAsStream(newCsvFilePath);

        List<String> lines = CSVFileUtil.getLines(inputStream, "UTF-8");
        List<Map<String, String>> mapList = CSVFileUtil.parseListoMap(lines);
        return mapList;
    }

    //适用于加载一列数据，并封装为List的场景
    public static List<String> loadCsvFileToList(String csvFile){
        String newCsvFilePath =  csvFile.replace("/","\\");
        //只支持加载resource目录下的文件
        InputStream inputStream = Loader.class.getClassLoader().getResourceAsStream(newCsvFilePath);

        List<String> lines = CSVFileUtil.getLines(inputStream, "UTF-8");
        return lines;
    }

    public static Map<String,Object> loadEnvFile(){
        HashMap<String,Object> result; ;
        Yaml yaml = new Yaml();
        Map<String, Object> load = new HashMap<>();
        InputStream inputStream = HttpRunner.class.getResourceAsStream("/env.yml");
        if(inputStream != null){
            load = yaml.loadAs(new InputStreamReader(
                            Objects.requireNonNull(HttpRunner.class.getResourceAsStream("/env.yml")), StandardCharsets.UTF_8),
                    Map.class
            );
        }

        return load;
    }


}
