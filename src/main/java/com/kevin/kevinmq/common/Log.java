package com.kevin.kevinmq.common;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Data
@TableName(value ="log")
public class Log implements Serializable {
	@TableId(type = IdType.AUTO)
	private Long logId;
	private Date date;
	private String action;
	private String info;

	public Log(String action, Object info) {
		this.action = action;
		if (info!=null) {
			this.info = info.toString();
		}
	}

	@TableField(exist = false)
	private static final long serialVersionUID = 1L;
}