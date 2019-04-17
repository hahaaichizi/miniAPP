package com.miniAPP.controller;

import com.miniAPP.mapper.FrCardMapper;
import com.miniAPP.mapper.FrLabelMapMapper;
import com.miniAPP.mapper.FrLabelMapper;
import com.miniAPP.pojo.FrCard;
import com.miniAPP.pojo.FrLabel;
import com.miniAPP.pojo.FrLabelMap;
import com.miniAPP.service.CardService;
import com.miniAPP.utils.JSONResult;
import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Api(value = "卡片API", tags = {"Card Controller"})
public class CardController extends BasicController {

    @Autowired
    private FrCardMapper cardMapper;

    @Autowired
    private FrLabelMapper labelMapper;

    @Autowired
    private FrLabelMapMapper labelMapMapper;

    @Autowired
    private CardService cardService;


    @ApiOperation(value = "获取用户的所有卡片", notes = "获取用户的所有卡片：通过指定用户ID")
    @ApiImplicitParams({@ApiImplicitParam(name = "userID", value = "userID", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "sessionToken", value = "sessionToken", required = true, dataType = "String", paramType = "query")})
    @ApiResponses({ @ApiResponse(code = 502, message = "Invalid Session Token"), @ApiResponse(code = 200, message = "ok") })
    @PostMapping("/getAllCardsByUserID")
    public JSONResult getAllCardsByUserID(String userID, String sessionToken){
        if(!sessionTokenIsValid(userID, sessionToken)){
            return JSONResult.errorMsg(INVALID_SESSION_TOKEN);
        }
        FrCard card=new FrCard();
        card.setUserId(userID);
        List<FrCard> cards=cardMapper.select(card);
        return JSONResult.ok(cards);
    }

    @ApiOperation(value = "获取用户的所有标签", notes = "获取用户的所有标签：通过指定用户ID")
    @ApiImplicitParams({@ApiImplicitParam(name = "userID", value = "userID", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "sessionToken", value = "sessionToken", required = true, dataType = "String", paramType = "query")})
    @ApiResponses({ @ApiResponse(code = 502, message = "Invalid Session Token"), @ApiResponse(code = 200, message = "ok") })
    @PostMapping("/getAllLablesByUserID")
    public JSONResult getAllLabelsByUserID(String userID, String sessionToken){
        if(!sessionTokenIsValid(userID, sessionToken)){
            return JSONResult.errorMsg(INVALID_SESSION_TOKEN);
        }
        FrLabel label=new FrLabel();
        label.setUserId(userID);
        List<FrLabel> labels=labelMapper.select(label);
        return JSONResult.ok(labels);
    }

    @ApiOperation(value = "获取用户的所有卡片", notes = "获取用户的所有卡片：通过指定卡片标签")
    @ApiImplicitParams({@ApiImplicitParam(name = "userID", value = "userID", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "labelContent", value = "labelContent", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "sessionToken", value = "sessionToken", required = true, dataType = "String", paramType = "query")})
    @ApiResponses({ @ApiResponse(code = 502, message = "Invalid Session Token"), @ApiResponse(code = 200, message = "ok") })
    @PostMapping("/getAllCardsByLabel")
    public JSONResult getAllCardsByLabel(String userID, String labelContent, String sessionToken){
        if(!sessionTokenIsValid(userID, sessionToken)){
            return JSONResult.errorMsg(INVALID_SESSION_TOKEN);
        }
        if(StringUtils.isBlank(labelContent)){
            labelContent="Genaral";
        }
        FrLabel label=new FrLabel();
        label.setUserId(userID);
        label.setLabelContent(labelContent);

        //得到符合要求的标签ID。通过用户ID和标签内容。fr_label(label_id, user_id, label_content)
        List<FrLabel> labels=labelMapper.select(label); //存放用户所有的标签ID

        //得到符合要求的卡片ID。通过标签ID。fr_label_Map(label_map_id, card_id, label_id)
        FrLabelMap labelMap=new FrLabelMap();
        List<String> cardIDs=new ArrayList<>(); //存放符合要求的所有卡片ID
        for(FrLabel l: labels) {
            labelMap.setLabelId(l.getLabelId());
            List<FrLabelMap> labelMaps=labelMapMapper.select(labelMap);
            cardIDs.add(labelMaps.get(0).getCardId());//注意：一个标签ID对应一张卡片ID
        }

        //得到符合要求的卡片。通过卡片ID。fr_card(...)
        FrCard card=new FrCard();
        List<FrCard> cards=new ArrayList<>(); //存放符合要求的用户的所有卡片
        for(String s: cardIDs){
            card.setCardId(s);
            cards.addAll(cardMapper.select(card));
        }

        return JSONResult.ok(cards);
    }

    @ApiOperation(value = "保存卡片", notes = "保存卡片：如有多个标签，以空格分割")
    @ApiImplicitParams({@ApiImplicitParam(name = "userID", value = "userID", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "card", value = "card", required = true, dataType = "FrCard", paramType = "query"),
            @ApiImplicitParam(name = "labelContent", value = "labelContent", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "sessionToken", value = "sessionToken", required = true, dataType = "String", paramType = "query")})
    @ApiResponses({ @ApiResponse(code = 502, message = "Invalid Session Token"), @ApiResponse(code = 200, message = "ok") })
    @PostMapping("/saveCard")
    public JSONResult saveCard(String userID, FrCard card, String labelContent, String sessionToken){
        if(!sessionTokenIsValid(userID, sessionToken)){
            return JSONResult.errorMsg(INVALID_SESSION_TOKEN);
        }

        String[] labelContents=labelContent.split(" ");
        card.setLabelNum(labelContents.length);
        String cardID=cardService.saveCard(card);
        cardService.saveLabel(userID ,cardID, labelContents);

        return JSONResult.ok();
    }
}