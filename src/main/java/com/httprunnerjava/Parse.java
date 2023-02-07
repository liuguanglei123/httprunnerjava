package com.httprunnerjava;

import com.httprunnerjava.model.component.atomsComponent.request.Variables;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class Parse {

    // 使用 $$ 表示 $
    static public Pattern dolloar_regex_compile = Pattern.compile("(\\$\\$).*");

    // 自定义变量表达式的正则匹配 ${var} or $var
    static public Pattern variable_regex_compile = Pattern.compile("\\$\\{(\\w+)}.*|\\$(\\w+).*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    // 自定义函数表达式的正则匹配 ${func1($var_1, $var_3)}
    static public Pattern function_regex_compile = Pattern.compile("(\\$\\{(\\w+)\\(([$\\w.\\-/\\s=,]*)\\)}).*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public static Set<String> regexFindallVariables(String content){
        Set<String> result = new HashSet<>();
        int match_start_position = 0;

        while(match_start_position < content.length()){
            Matcher var_match = variable_regex_compile.matcher(content);
            if(var_match.find(match_start_position)) {
                String var_name;
                if(var_match.group(1) == null || var_match.group(1).equals(""))
                    var_name = var_match.group(2);
                else
                    var_name = var_match.group(1);

                result.add(var_name);
            }

            int curr_position = match_start_position;
            match_start_position = content.indexOf("$", curr_position+1);
            if(match_start_position == -1){
                content = content.substring(curr_position);
                match_start_position = content.length();
            }else{
                content = content.substring(curr_position,match_start_position);
            }
        }

        return result;
    }

}
