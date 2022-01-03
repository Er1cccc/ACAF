package com.er1cccc.acaf.core.resolver.impl;

import com.er1cccc.acaf.config.Sink;
import com.er1cccc.acaf.core.entity.Instruction;
import com.er1cccc.acaf.core.resolver.Resolver;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CompositResolver implements Resolver {
    List<Resolver> resolverList=new ArrayList<>();
    int ind=0;
    Sink sink;

    public CompositResolver(Sink sink) {
        this.sink = sink;
    }

    @Override
    public boolean resolve(Instruction instruction) {
        int size=resolverList.size();

        if(ind==size){
            return true;
        }
        if(instruction!=null){
            Resolver resolver = resolverList.get(ind);
            if(resolver.resolve(instruction)){
                ind++;
                return true;
            }
        }
        return false;
    }

    public void addResolver(Resolver resolver){
        resolverList.add(resolver);
    }

    public void reset(){
        ind=0;
    }

}
