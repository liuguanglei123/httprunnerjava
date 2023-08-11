package com.httprunnerjava.model.component.atomsComponent.response;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Export implements Serializable  {

    private List<String> content = new ArrayList<>();

    public Export(String export_var_name_strlist){
        content = JSONArray.parseArray(export_var_name_strlist,String.class);
    }

    public void update(Export export){
        this.content.addAll(export.getContent());
    }
}
