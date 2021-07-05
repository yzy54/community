package com.nowcoder.community.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class SensitiveFilter {

    //需要把敏感词替换成的符号
    private static final String REPLACEMENT = "***";

    //根节点初始化
    private TrieNode rootNode = new TrieNode();

    @PostConstruct
    public void init(){
        try(InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive_words.txt");
            //将字符流转换为缓冲流
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        ) {
            String keyword;
            while((keyword= reader.readLine()) != null){
                //将keyword添加到前缀树
                this.addKeyword(keyword);
            }
        } catch (Exception e) {
            log.error("加载敏感词文件失败" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addKeyword(String keyword){
        TrieNode tempNode = rootNode;
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);
            if(subNode == null){
                //初始化子节点
                subNode = new TrieNode();
                tempNode.addSubNode(c, subNode);
            }

            //让指针指向子节点，进入下一轮循环
            tempNode = subNode;

            //设置结束标识
            if(i == keyword.length() - 1){
                tempNode.setKeywordEnd(true);
            }
        }
    }

    /**
     * 过滤敏感词
     * @param 需要过滤的文本
     * @return 过滤后的文本
     */
    public String filter(String text){
        if(StringUtils.isBlank(text)){
            return null;
        }

        //指针1
        TrieNode tempNode = rootNode;
        //指针2
        int begin = 0;
        //指针3
        int position = 0;
        //结果
        StringBuilder builder = new StringBuilder();
        while(position< text.length()){
            char c = text.charAt(position);

            //跳过符号
            if(isSymbol(c)){
                //若指针1处于根节点，将此符号计入结果，让指针2向下走一步
                if(tempNode == rootNode){
                    builder.append(c);
                    begin++;
                }
                position++;
                continue;
            }

            //检查下级节点
            tempNode = tempNode.getSubNode(c);
            if(tempNode == null){
                //以begin开头的字符串不是敏感词
                builder.append(text.charAt(begin));
                position = ++begin;
                //指针三重新指向新节点
                tempNode = rootNode;
            }else if(tempNode.isKeywordEnd()){
                //发现铭感次，经begin-position字符串替换
                builder.append(REPLACEMENT);
                //进入下一个位置
                begin = ++position;
                //指针三重新指向新节点
                tempNode = rootNode;
            }else{
                //检查下一个字符
                position++;
            }

        }

        //将最后一批字符计入结果
        builder.append(text.substring(begin));
        return builder.toString();
    }

    //判断是否为符号
    private boolean isSymbol(Character c){
        return !CharUtils.isAsciiAlphanumeric(c) && (c<0x2E80 || c > 0x9FFF);
    }


    //前缀树
    private class TrieNode{
        //关键词结束表时
        private boolean isKeywordEnd = false;

        //子节点(key是下级节点的字符, value是下级节点)
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        //添加子节点
        public void addSubNode(Character c, TrieNode node){
            subNodes.put(c, node);
        }

        //获取子节点
        public TrieNode getSubNode(Character c){
            return subNodes.get(c);
        }
    }



}
