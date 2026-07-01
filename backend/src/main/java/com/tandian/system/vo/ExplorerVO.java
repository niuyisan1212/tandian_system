package com.tandian.system.vo;

import com.tandian.system.entity.Explorer;
import lombok.Data;
import java.io.Serializable;

/**
 * 探店员精简VO
 */
@Data
public class ExplorerVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String phone;

    public static ExplorerVO fromEntity(Explorer explorer) {
        if (explorer == null) return null;
        ExplorerVO vo = new ExplorerVO();
        vo.setId(explorer.getId());
        vo.setName(explorer.getName());
        vo.setPhone(explorer.getPhone());
        return vo;
    }
}
