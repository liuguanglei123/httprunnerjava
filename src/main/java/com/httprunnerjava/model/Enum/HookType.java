package com.httprunnerjava.model.Enum;

import java.io.Serializable;

public enum HookType implements Serializable  {
        StringHook(1),
        MapHook(2);

        private final Integer type;

        HookType(Integer type){
            this.type=type;
        }

        public Integer getType(){
            return this.type;
        }

        public static HookType getHookType(Integer type){
            for (HookType c : HookType.values()) {
                if (c.getType().equals(type)) {
                    return c;
                }
            }
            return null;
        }

        public Boolean equals(HookType hookType){
            if(this.type.equals(hookType.getType())){
                return true;
            }else{
                return false;
            }
        }
    }