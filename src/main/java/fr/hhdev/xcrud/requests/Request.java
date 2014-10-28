/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.hhdev.xcrud.requests;

/**
 *
 * @author HHFrancois
 * @param <T>
 */
public class Request<T> {
	private Class<T> type = null;
	private T lt = null;
	private T gt = null;
	private T notlt = null;
	private T notgt = null;
	private T[] likeCriteria;
	private T[] notLikeCriteria;

	public void addLtGtCriteria(T lt, T gt) {
		if(lt!=null&&gt!=null) {
			this.lt = lt;
			this.gt = gt;
			setTypeFromEntity(lt);
		}
	}

	public void addExcludeLtGtCriteria(T lt, T gt) {
		if(lt!=null&&gt!=null) {
			this.setNotlt(lt);
			this.setNotgt(gt);
			setTypeFromEntity(lt);
		}
	}

	public void addLikeCriteria(T... criteria) {
		this.likeCriteria = criteria;
		if(criteria!=null && criteria.length >0)
			setTypeFromEntity(criteria[0]);
	}

	public void addNotLikeCriteria(T... criteria) {
		this.notLikeCriteria = criteria;
		if(criteria!=null && criteria.length >0)
			setTypeFromEntity(criteria[0]);
	}

	/**
	 * @return the lt
	 */
	public T getLt() {
		return lt;
	}

	/**
	 * @return the gt
	 */
	public T getGt() {
		return gt;
	}

	/**
	 * @return the likeCriteria
	 */
	public T[] getLikeCriteria() {
		return likeCriteria;
	}

	/**
	 * @return the notLikeCriteria
	 */
	public T[] getNotLikeCriteria() {
		return notLikeCriteria;
	}

	/**
	 * @return the tClass
	 */
	public Class<T> getType() {
		return type;
	}

	private void setTypeFromEntity(T entity) {
		if(type==null) {
			if (entity != null) {
				type = (Class<T>) entity.getClass();
			}
		}
	}
	public void setType(Class<T> tClass) {
		this.type = tClass;
	}

	/**
	 * @return the notlt
	 */ public T getNotlt() {
		return notlt;
	}

	/**
	 * @param notlt the notlt to set
	 */ public void setNotlt(T notlt) {
		this.notlt = notlt;
	}

	/**
	 * @return the notgt
	 */ public T getNotgt() {
		return notgt;
	}

	/**
	 * @param notgt the notgt to set
	 */ public void setNotgt(T notgt) {
		this.notgt = notgt;
	}

}
