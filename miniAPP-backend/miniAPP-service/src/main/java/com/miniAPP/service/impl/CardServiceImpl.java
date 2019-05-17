package com.miniAPP.service.impl;

import com.miniAPP.mapper.FrCardMapper;
import com.miniAPP.mapper.FrLabelMapMapper;
import com.miniAPP.mapper.FrLabelMapper;
import com.miniAPP.mapper.FrUserInfoMapper;
import com.miniAPP.pojo.FrCard;
import com.miniAPP.pojo.FrLabel;
import com.miniAPP.pojo.FrLabelMap;
import com.miniAPP.pojo.FrUserInfo;
import com.miniAPP.service.CardService;
import com.miniAPP.utils.JSONResult;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.ocr.v20181119.OcrClient;
import com.tencentcloudapi.ocr.v20181119.models.GeneralFastOCRRequest;
import com.tencentcloudapi.ocr.v20181119.models.GeneralFastOCRResponse;
import com.tencentcloudapi.ocr.v20181119.models.TextDetection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service
public class CardServiceImpl implements CardService {

    @Autowired
    private FrCardMapper cardMapper;

    @Autowired
    private FrLabelMapper labelMapper;

    @Autowired
    private FrLabelMapMapper labelMapMapper;

    @Autowired
    private FrUserInfoMapper userInfoMapper;

    private static final long forgettingCurve[] = {
            0,                  // 0->创建日
            1*24*60*60*1000L,	// 1->1天
            2*24*60*60*1000L,	// 2->2天
            4*24*60*60*1000L,	// 3->4天
            7*24*60*60*1000L,	// 4->7天
            15*24*60*60*1000L,	// 5->15天
            31*24*60*60*1000L	// 6->31天
                                // 7->熟记
    };

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Long saveCard(FrCard c){

        Long userID = c.getUserId();
        FrUserInfo userInfo = userInfoMapper.selectByPrimaryKey(userID);
        StringBuilder cardIDstr = new StringBuilder(userID.toString());
        cardIDstr.append(String.format("%05d", userInfo.getTotalCards()));
        Long cardId = Long.parseLong(cardIDstr.toString());

        c.setCardId(cardId);
        c.setRememberTimes(0);
        c.setMemoLevel((byte)0);
        Date now = new Date(System.currentTimeMillis());
        c.setLastRememberTime(now);
        c.setCreateTime(now);
        c = nextTime(c, false);

        userInfo.setTotalCards(userInfo.getTotalCards()+1);

        cardMapper.insert(c);
        userInfoMapper.updateByPrimaryKeySelective(userInfo);

        return c.getCardId();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void saveLabel(Long userID, Long cardID, String[] labelContents){
        FrLabel label=new FrLabel();
        FrLabelMap labelMap=new FrLabelMap();

        label.setUserId(userID);
        labelMap.setCardId(cardID);

        if(labelContents.length==0){
            label.setLabelContent("Genaral");
            initLabel(label, labelMap);
        }
        else {
            for (String s : labelContents) {
                label.setLabelContent(s);
                initLabel(label, labelMap);
            }
        }
    }


    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void initLabel(FrLabel label, FrLabelMap labelMap){

        FrLabel temp = labelMapper.selectOne(label);
        if(temp == null){
            labelMapper.insert(label);
            label = labelMapper.selectOne(label);
        }
        else{
            label = temp;
        }
        labelMap.setLabelId(label.getLabelId());
        labelMapMapper.insert(labelMap);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public FrCard queryCardByCardID(Long cardID){

        FrCard card = new FrCard();
        card.setCardId(cardID);
        return cardMapper.selectOne(card);
    }

    /**
     *
     * @param frCard 当前卡片
     * @param forget 是否忘记：false：没忘记 true：忘记
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public FrCard nextTime(FrCard frCard, boolean forget){

        byte currMemoLevel = frCard.getMemoLevel();
        if(!forget){
            if(currMemoLevel < 6){
                currMemoLevel = (byte)(currMemoLevel+1);
                frCard.setMemoLevel(currMemoLevel);
                frCard.setNextTime(new Date(System.currentTimeMillis() + forgettingCurve[currMemoLevel]));
                cardMapper.updateByPrimaryKeySelective(frCard);
            }
            else if(currMemoLevel == 6){
                frCard.setMemoLevel((byte)(currMemoLevel+1));
                cardMapper.updateByPrimaryKeySelective(frCard);
            }
        }
        else {
            currMemoLevel = (byte)1;
            frCard.setMemoLevel(currMemoLevel);
            frCard.setNextTime(new Date(System.currentTimeMillis() + forgettingCurve[currMemoLevel]));
            cardMapper.updateByPrimaryKeySelective(frCard);
        }
        frCard.setRememberTimes(frCard.getRememberTimes()+1);
        frCard.setLastRememberTime(new Date(System.currentTimeMillis()));
        return frCard;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<Long> queryUserNeededToBeNoticed(){

        return cardMapper.queryNoticedUserList();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public int queryUnfamiliarCardNum(Long userID){

        int totalCardNum = userInfoMapper.selectByPrimaryKey(userID).getTotalCards();
        int unfamiliarCardNum = cardMapper.queryFamiliarCardNum(userID);
        return totalCardNum-unfamiliarCardNum;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<FrCard> getUnFamiliarCard(Long userID){

        return cardMapper.queryUnFamiliarCard(userID);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public String Ocr(String picUrl){
        try{
            Credential cred = new Credential("AKIDJZOXdbJNfj1vb2uJ8dwGO0Vi5Iy5vlL1", "Shja5LVkneQPnhvwV6xe9RmFa2dfprBo");
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("ocr.tencentcloudapi.com");
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            OcrClient client = new OcrClient(cred, "ap-guangzhou", clientProfile);
            String params = "{\"ImageUrl\":\""+picUrl+"\"}";
            GeneralFastOCRRequest req = GeneralFastOCRRequest.fromJsonString(params, GeneralFastOCRRequest.class);
            GeneralFastOCRResponse resp = client.GeneralFastOCR(req);

            TextDetection[] textDetections=resp.getTextDetections();
            String text="";
            for(int i=0;i<textDetections.length;i++){
                if(i>0 && textDetections[i].getAdvancedInfo().compareTo(textDetections[i-1].getAdvancedInfo())!=0) text+="\n";
                text+=textDetections[i].getDetectedText();
            }
            return text;

        } catch (TencentCloudSDKException e) {
            return null;
        }
    }
}
