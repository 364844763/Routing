package com.hit.jj.draw;

import java.util.Enumeration;
import java.util.Vector;
/**
 * 
 * @author ropp gispace@yeah.net
 *
 * ��������ĸ���
 * DrawTool�̳�������࣬�߱�����Ӽ���������
 */
public class Subject {

private Vector<DrawEventListener> repository = new Vector<DrawEventListener>();

	// ��Ӽ���
	public void addEventListener(DrawEventListener listener) {
		this.repository.addElement(listener);
	}
	
	// �Ƴ�����
	public void removeEventListener(DrawEventListener listener){
		this.repository.removeElement(listener);
	}

	// ��������ɷ���Ϣ
	public void notifyEvent(DrawEvent event) {
		Enumeration<DrawEventListener> en = this.repository.elements();
		while (en.hasMoreElements()) {
			DrawEventListener listener = en.nextElement();
			listener.handleDrawEvent(event);
		}
	}
}
