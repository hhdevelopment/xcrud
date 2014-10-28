/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.hhdev.xcrud;

import fr.hhdev.xcrud.annotations.OrderBy;
import fr.hhdev.xcrud.exceptions.BooleanValueException;
import fr.hhdev.xcrud.exceptions.DateValueException;
import fr.hhdev.xcrud.exceptions.EntityIdNotFound;
import fr.hhdev.xcrud.exceptions.FieldNotFound;
import fr.hhdev.xcrud.exceptions.InvalidIdException;
import fr.hhdev.xcrud.exceptions.MethodNotFound;
import fr.hhdev.xcrud.exceptions.NoVariableException;
import fr.hhdev.xcrud.exceptions.NullValueException;
import fr.hhdev.xcrud.exceptions.NumberValueException;
import fr.hhdev.xcrud.requests.Request;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

/**
 *
 * @author francois
 * @param <T>
 */
public abstract class XCrud<T> {

	@Inject
	protected Logger logger;

	protected abstract EntityManager getEntityManager();

	private Class<T> getEntityClass() {
		Type genericSuperclass = this.getClass().getGenericSuperclass();
		ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
		Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
		Type actualTypeArgument = actualTypeArguments[0];
		return (Class<T>) actualTypeArgument;
	}

	/**
	 * Retourne la valeur de configuration de la base mysql
	 *
	 * @param name
	 * @return
	 * @throws fr.hhdev.xcrud.exceptions.NoVariableException
	 */
	public String getMysqlVariable(String name) throws NoVariableException {
		Query query = getEntityManager().createNativeQuery("show variables");
		List<Object[]> list = query.getResultList();
		for (Object[] objects : list) {
			if (name.equals("" + objects[0])) {
				return "" + objects[1];
			}
		}
		throw new NoVariableException(name);
	}

	/**
	 * Retourne si un index est present sur la table donnée
	 *
	 * @param tableName
	 * @param name
	 * @return
	 */
	public boolean isIndexPresent(String tableName, String name) {
		Query query = getEntityManager().createNativeQuery("show indexes");
		List<Object[]> list = query.getResultList();
		for (Object[] objects : list) {
			if (name.equals("" + objects[2])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Retourne le nom SQL de la table de la classe passée en argument
	 *
	 * @return
	 */
	public String getSQLTableName() {
		return getSQLTableName(getEntityClass());
	}

	/**
	 * Retourne le nom SQL de la table de la classe passée en argument
	 *
	 * @param tClass
	 * @return
	 */
	public String getSQLTableName(Class tClass) {
		String sqlName = null;
		if (tClass.isAnnotationPresent(Table.class)) {
			Table table = (Table) tClass.getAnnotation(Table.class);
			sqlName = table.name();
		}
		if (sqlName == null || sqlName.isEmpty()) {
			sqlName = tClass.getSimpleName().toUpperCase();
		}
		return sqlName;
	}

	/**
	 * Retourne le nom de la table de l'objet
	 *
	 * @param tClass
	 * @return
	 */
	private String getTableName(Class tClass) {
		return tClass.getSimpleName();
	}

	private String getStatement(final TypeStatement type, Class tClass) {
		Class tempClass = tClass;
		while (!tClass.isAnnotationPresent(Entity.class)) {
			tClass = tClass.getSuperclass();
			if (tClass.equals(Object.class)) {
				throw new IllegalArgumentException("Unknown entity type [" + tempClass.getSimpleName() + "].");
			}
		}
		if (type.equals(TypeStatement.COUNT)) {
			return "select count(distinct o) from " + getTableName(tClass) + " as o";
		} else if (type.equals(TypeStatement.SELECT)) {
			return "select distinct object(o) from " + getTableName(tClass) + " as o";
		} else if (type.equals(TypeStatement.DELETE)) {
			return "delete from " + getTableName(tClass) + " as o";
		} else {
			return "";
		}
	}

	private void addOrderByStatement(StringBuilder statement, final TypeStatement type, final Class tClass) {
		if (type.equals(TypeStatement.SELECT) && tClass.isAnnotationPresent(OrderBy.class)) {
			OrderBy orderBy = (OrderBy) tClass.getAnnotation(OrderBy.class);
			String[] orders = orderBy.values();
			if (orders.length > 0) {
				statement.append(" order by");
				int nb = 0;
				for (String ord : orders) {
					statement.append(" o.").append(ord);
					if (++nb < orders.length) {
						statement.append(",");
					}
				}
				statement.append(" ").append(orderBy.asc() ? Constants.ASC:Constants.DESC);
			}
		}
	}

	/**
	 * Detache l'entité du contexte de peristance
	 *
	 * @param entity
	 */
	public void detach(final T entity) {
		getEntityManager().detach(entity);
	}

	/**
	 * Reload d'une entité
	 *
	 * @param entity
	 */
	public void refresh(final T entity) {
		getEntityManager().refresh(entity);
	}

	/**
	 * Creation d'une entité
	 *
	 * @param entity
	 */
	public void persist(final T entity) {
		getEntityManager().persist(entity);
	}

	/**
	 * mise à jour d'une entité
	 *
	 * @param entity
	 * @return T
	 */
	public T merge(final T entity) {
		return getEntityManager().merge(entity);
	}

	/**
	 * Suppression d'une entité
	 *
	 * @param entity
	 */
	public void remove(final T entity) {
		getEntityManager().remove(getEntityManager().merge(entity));
	}

	/**
	 * Suppression d'un ensemble d'entité similaire à l'entité passé en argument on ne passe plus par getQueryFromRequest(TypeStatement.DELETE, 0, 0, request);
	 * pour s'affranchir d'un probleme de l'api appelante ou l'entité serait sans discriminant et supprimerait toutes les entités de la table
	 *
	 * @param entity
	 */
	public void removeAllLike(final T entity) {
		Request<T> request = new Request<T>();
		request.addLikeCriteria(entity);
		Map<String, Object> values = new HashMap<String, Object>();
		String queryString = getQueryStringFromRequest(TypeStatement.DELETE, request, values);
		Query query = getEntityManager().createQuery(queryString);
		try {
			if (!values.isEmpty()) {
				setValuesToQuery(query, values);
				query.executeUpdate();
			} else {
				throw new IllegalArgumentException();
			}
		} catch (IllegalArgumentException iae) {
			logger.error("Tentative d'execution d'un removeAllLike sans parametre discriminant : " + entity.getClass().getSimpleName(), iae);
		}
	}

	/**
	 * Trouve une entité par son id
	 *
	 * @param id
	 * @return T
	 */
	public T find(final Object id) {
		Class tClass = getEntityClass();
		return (T) _find(tClass, id);
	}

	/**
	 * Trouve une entité au hasard
	 *
	 * @return T
	 */
	public T findRandom() {
		Class tClass = getEntityClass();
		int total = getNumberEntity();
		int idx = (int) ((Math.random() * total) + 1);
		Request<T> request = new Request<T>();
		request.setType(tClass);
		Query query = getQueryFromRequest(TypeStatement.SELECT, idx, 1, request);
		try {
			return (T) query.getSingleResult();
		} catch (NoResultException ex) {
			return null;
		}
	}

	/**
	 * nombre d'entités de type tClass
	 *
	 * @return Long
	 */
	public int getNumberEntity() {
		Class tClass = getEntityClass();
		CriteriaQuery cq = getEntityManager().getCriteriaBuilder().createQuery();
		Root<T> rt = cq.from(tClass);
		cq.select(getEntityManager().getCriteriaBuilder().count(rt));
		Query q = getEntityManager().createQuery(cq);
		return ((Long) q.getSingleResult()).intValue();
	}

	/**
	 * supprime toutes les entités en base.
	 */
	public void removeAll() {
		getEntityManager().createQuery(getStatement(TypeStatement.DELETE, getEntityClass())).executeUpdate();
	}

	/**
	 * Trouve toutes les entités de type tClass
	 *
	 * @return List<T>
	 */
	public List<T> findAll() {
		return findAll(0, 0);
	}

	/**
	 * Trouve toutes les entités de type tClass avec pagination
	 *
	 * @param page : numéro de page
	 * @param number : nombre d'éléments par page
	 * @return List<T>
	 */
	public List<T> findAll(final int page, final int number) {
		Class tClass = getEntityClass();
		Request<T> request = new Request<T>();
		request.setType(tClass);
		Query query = getQueryFromRequest(TypeStatement.SELECT, page, number, request);
		return query.getResultList();
	}

	/**
	 * Retourne le nombre d'element de la recherche.
	 *
	 * @param entity
	 * @return
	 */
	public int getNumberEntityLike(final T entity) {
		Class tClass = entity.getClass();
		Request<T> request = new Request<T>();
		request.setType(tClass);
		request.addLikeCriteria(entity);
		Query query = getQueryFromRequest(TypeStatement.COUNT, 0, 0, request);
		return ((Long) query.getSingleResult()).intValue();
	}

	/**
	 * Retourne le numéro de l'élement.
	 *
	 * @param entity
	 * @param lt
	 * @param gt
	 * @param criteria
	 * @return
	 */
	public Long getIndexEntityInAllLike(final T entity, final T lt, final T gt, final T... criteria) {
		List<T> list = findAllLike(lt, gt, criteria);
		return (long) list.indexOf(entity);
	}

	/**
	 * Retourne l'élement suivant.
	 *
	 * @param entity
	 * @param lt
	 * @param gt
	 * @param criteria
	 * @return
	 */
	public T getNextEntityInAllLike(final T entity, final T lt, final T gt, final T... criteria) {
		List<T> list = findAllLike(lt, gt, criteria);
		int index = list.indexOf(entity);
		if (index == list.size() - 1) {
			return null;
		}
		return list.get(index + 1);
	}

	/**
	 * Retourne l'élement precedent.
	 *
	 * @param entity
	 * @param lt
	 * @param gt
	 * @param criteria
	 * @return
	 */
	public T getPreviousEntityInAllLike(final T entity, final T lt, final T gt, final T... criteria) {
		List<T> list = findAllLike(lt, gt, criteria);
		int index = list.indexOf(entity);
		if (index == 0) {
			return null;
		}
		return list.get(index - 1);
	}

	/**
	 * Recherche simple, trouve toutes les entités similaires à l'entité passé en argument
	 *
	 * @param entity
	 * @return List<T>
	 */
	public List<T> findAllLike(final T entity) {
		return findAllLike(0, 0, entity);
	}

	/**
	 * Recherche simple, trouve toutes les entités similaires à l'entité passé en argument avec pagination
	 *
	 * @param page : numéro de page
	 * @param number : nombre d'éléments par page
	 * @param entity
	 * @return List<T>
	 */
	public List<T> findAllLike(final int page, final int number, final T entity) {
		return findAllLike(page, number, null, null, entity);
	}

	/**
	 * Retourne la premiere entité similaire à l'entité passé en argument
	 *
	 * @param entity
	 * @return T
	 */
	public T findLike(final T entity) {
		Class tClass = entity.getClass();
		try {
			Object id = getIdValue(entity);
			return (T) _find(tClass, id); // on a trouvé l'entité par son id
		} catch (InvalidIdException ex) { // sinon on essaye de trouvé l'entité par ses fields qui ont une valeur
			List<T> list = findAllLike(1, 1, null, null, entity);
			if (list.size() > 0) {
				return list.get(0);
			}
		}
		return null;
	}

	/**
	 * Retourne l'unique entité similaire à l'entité passé en argument Leve une exception si pas trouvé ou plusieurs reponses
	 *
	 * @param entity
	 * @return T
	 */
	public T findUniqueLike(final T entity) {
		return (T) _findUniqueLike(entity);
	}

	/**
	 * Trouve une entité par son id
	 *
	 * @param tClass
	 * @param id
	 * @return
	 */
	private Object _find(final Class tClass, final Object id) {
		return getEntityManager().find(tClass, id);
	}

	/**
	 * Retourne l'id de l'entité Leve une exception si pas trouvé
	 *
	 * @param entity
	 * @return
	 */
	private Object getIdValue(final Object entity) throws InvalidIdException {
		try {
			Method idGetter = EntityTools.getIdentityGetter(entity);
			Object obj = idGetter.invoke(entity);
			if (!checkValue(obj)) {
				throw new InvalidIdException();
			}
			return obj;
		} catch (EntityIdNotFound e) {
		} catch (MethodNotFound e) {
		} catch (IllegalAccessException e) {
		} catch (IllegalArgumentException e) {
		} catch (InvocationTargetException e) {
		} catch (InvalidIdException e) {
		}
		throw new InvalidIdException();
	}

	/**
	 * Retourne l'unique entité similaire à l'entité passé en argument Leve une exception si pas trouvé ou plusieurs reponses
	 *
	 * @param entity
	 * @return
	 */
	private Object _findUniqueLike(final Object entity) throws NoResultException, NonUniqueResultException {
		Class tClass = entity.getClass();
		try {
			Object id = getIdValue(entity);
			return _find(tClass, id); // on a trouvé l'entité par son id
		} catch (InvalidIdException ex) { // sinon on essaye de trouvé l'entité par ses fields qui ont une valeur
			Request request = new Request();
			request.addLikeCriteria(entity);
			Query query = getQueryFromRequest(TypeStatement.SELECT, 0, 0, request);
			return query.getSingleResult();
		}
	}

	/**
	 * Retourne le nombre d'element de la recherche.
	 *
	 * @param lt : correspont aux bornes inferieurs pour les fields de type Number ou Date
	 * @param gt : correspont aux bornes superieurs pour les fields de type Number ou Date
	 * @param criteria : ensemble d'entité dont les fields définit serviront pour la requete
	 * @return
	 */
	public int getNumberEntityLike(final T lt, final T gt, final T... criteria) {
		Request<T> request = new Request<T>();
		request.addLtGtCriteria(lt, gt); // set le type automatiquement
		request.addLikeCriteria(criteria); // set le type automatiquement
		Query query = getQueryFromRequest(TypeStatement.COUNT, 0, 0, request);
		return ((Long) query.getSingleResult()).intValue();
	}

	/**
	 * Recherche multicriteres.
	 *
	 * @param lt : correspont aux bornes inferieurs pour les fields de type Number ou Date
	 * @param gt : correspont aux bornes superieurs pour les fields de type Number ou Date
	 * @param criteria : ensemble d'entité dont les fields définit serviront pour la requete
	 * @return List<T>
	 */
	public List<T> findAllLike(final T lt, final T gt, final T... criteria) {
		return findAllLike(0, 0, lt, gt, criteria);
	}

	/**
	 * Recherche multicriteres avec pagination.
	 *
	 * @param page : numéro de page
	 * @param number : nombre d'éléments par page
	 * @param lt : correspont aux bornes inferieurs pour les fields de type Number ou Date
	 * @param gt : correspont aux bornes superieurs pour les fields de type Number ou Date
	 * @param criteria : ensemble d'entité dont les fields définit serviront pour la requete
	 * @return
	 */
	public List<T> findAllLike(final int page, final int number, final T lt, final T gt, final T... criteria) {
		Request<T> request = new Request<T>();
		request.addLtGtCriteria(lt, gt); // set le type automatiquement
		request.addLikeCriteria(criteria); // set le type automatiquement
		return find(page, number, request);
	}

	/**
	 * Recherche multicriteres avec pagination.
	 *
	 * @param page : numéro de page
	 * @param number : nombre d'éléments par page
	 * @param lt : correspont aux bornes inferieurs pour les fields de type Number ou Date
	 * @param gt : correspont aux bornes superieurs pour les fields de type Number ou Date
	 * @param criteria : ensemble d'entité dont les fields définit serviront pour la requete
	 * @return
	 */
	public List<T> findUnLike(final T lt, final T gt, final int page, final int number, final T... criteria) {
		Request<T> request = new Request<T>();
		request.addExcludeLtGtCriteria(lt, gt); // set le type automatiquement
		request.addNotLikeCriteria(criteria); // set le type automatiquement
		return find(page, number, request);
	}

	/**
	 * Recherche multicriteres avec pagination.
	 *
	 * @param page : numéro de page
	 * @param number : nombre d'éléments par page
	 * @param criteria : ensemble d'entité dont les fields définit serviront pour la requete
	 * @return
	 */
	public List<T> findUnLike(final int page, final int number, final T... criteria) {
		return findUnLike(null, null, page, number, criteria);
	}

	/**
	 * Recherche multicriteres avec pagination.
	 *
	 * @param criteria : ensemble d'entité dont les fields définit serviront pour la requete
	 * @return
	 */
	public List<T> findUnLike(final T... criteria) {
		return findUnLike(0, 0, criteria);
	}

	/**
	 * Recherche multicriteres.
	 *
	 * @param page : numéro de page
	 * @param number : nombre d'éléments par page
	 * @param request
	 * @return List<T>
	 */
	private List<T> find(final int page, final int number, final Request<T> request) {
		return (List<T>) getQueryFromRequest(TypeStatement.SELECT, page, number, request).getResultList();
	}

	/**
	 * Recherche multicriteres.
	 *
	 * @param type : type de query (count/select...)
	 * @param page : numéro de page
	 * @param number : nombre d'éléments par page
	 * @param request
	 * @return List<T>
	 */
	private Query getQueryFromRequest(final TypeStatement type, final int page, final int number, final Request<T> request) {
		Map<String, Object> values = new HashMap<String, Object>();
		String queryString = getQueryStringFromRequest(type, request, values);
		Query query = getEntityManager().createQuery(queryString);
		setValuesToQuery(query, values);
		this.setPaginations(page, number, query);
		return query;
	}

	/**
	 * Recherche multicriteres.
	 *
	 * @param type : type de query (count/select...)
	 * @param request
	 * @return String
	 */
	private String getQueryStringFromRequest(final TypeStatement type, final Request<T> request, final Map<String, Object> values) {
		Class<T> tClass = request.getType();
		StringBuilder queryString = new StringBuilder();
		queryString.append(this.getStatement(type, tClass));

		String predicatsBetween = getBetweenPredicatsAndValues(false, values, "o", tClass, request.getLt(), request.getGt());
		String predicatsNotBetween = getBetweenPredicatsAndValues(true, values, "o", tClass, request.getNotlt(), request.getNotgt());
		String predicatsEquals = getInPredicatsAndValues(false, values, "o", tClass, request.getLikeCriteria());
		String predicatsNotEquals = getInPredicatsAndValues(true, values, "o", tClass, request.getNotLikeCriteria());

		String joinString = Constants._WHERE_;
		if (!predicatsBetween.isEmpty()) {
			queryString.append(joinString);
			joinString = Constants._AND_;
			queryString.append(predicatsBetween);
		}
		if (!predicatsNotBetween.isEmpty()) {
			queryString.append(joinString);
			joinString = Constants._AND_;
			queryString.append(predicatsNotBetween);
		}
		if (!predicatsEquals.isEmpty()) {
			queryString.append(joinString);
			joinString = Constants._AND_;
			queryString.append(predicatsEquals);
		}
		if (!predicatsNotEquals.isEmpty()) {
			queryString.append(joinString);
			queryString.append(predicatsNotEquals);
		}
		this.addOrderByStatement(queryString, type, tClass);
		logger.debug("--- REQUETE SQL : \n : {}", queryString.toString());
		return queryString.toString();
	}

	/**
	 * Met en place la pagination sur le query
	 *
	 * @param page : numéro de page
	 * @param number : nombre d'éléments par page
	 * @param query : l'objet requete
	 */
	private void setPaginations(final int page, final int number, final Query query) {
		if (page > 0 && number > 0) {
			query.setFirstResult(((page - 1) * number));
			query.setMaxResults(number);
		}
	}

	/**
	 * Genere un ensemble de predicats de type between séparés par des 'and', et met a jour la hashmap
	 *
	 * @param values : Map qui contiendra les valeurs des objets constituant les prédicat
	 * @param t : label de l'objet de la requete
	 * @param tClass : classe de l'objet de la requete
	 * @param lt : entité possedant les valeurs de borne inferieur
	 * @param gt : entité possedant les valeurs de borne superieur
	 * @return prédicats
	 */
	private String getBetweenPredicatsAndValues(final boolean not, final Map<String, Object> values, final String t, final Class tClass, final T lt, final T gt) {
		String predicats = "";
		if (lt != null && gt != null) {
			List<String> andList = new ArrayList<String>();
			for (Method getter : tClass.getMethods()) {
				try {
					Field field = EntityTools.getFieldFromGetter(tClass, getter);
					if (!field.isAnnotationPresent(Transient.class)) {
						try {
							Object ltValue = getter.invoke(lt);
							Object gtValue = getter.invoke(gt);
							try {
								checkBetweenValue(ltValue, gtValue);
								// TODO ici on pourait traiter le cas avec les jointures.
								// Cas à étudier, loin d'etre simple.
								// father.children.a > 5 and father.children.a < 10 and father.children.b > 10 and father.children.b < 50
							} catch (NullValueException ex) {
								andList.add((not ? "not " : " ") + t + "." + field.getName() + " is null");
							} catch (NullPointerException ex) {
							} catch (BooleanValueException ex) {
								if (!ltValue.equals(gtValue)) {
									int index = values.size();
									values.put("bool_" + index, gtValue);
									andList.add((not ? "not " : " ") + t + "." + field.getName() + " = :bool_" + index);
								}
							} catch (DateValueException ex) {
								addBetweenPredicat(not, values, andList, ltValue, gtValue, field.getName(), t);
							} catch (NumberValueException ex) {
								addBetweenPredicat(not, values, andList, ltValue, gtValue, field.getName(), t);
							}
						} catch (IllegalAccessException ex) {
//							log.error("IllegalAccessException : " + tClass.getSimpleName() + "." + getter.getName(), ex);
						} catch (IllegalArgumentException ex) {
//							log.error("IllegalArgumentException : " + tClass.getSimpleName() + "." + getter.getName(), ex);
						} catch (InvocationTargetException ex) {
//							log.error("InvocationTargetException : " + tClass.getSimpleName() + "." + getter.getName(), ex);
						}
					}
				} catch (FieldNotFound ex) {
					// Ne pas logger cette exception, c'est normal.
					// Qui dit exception, ne veut pas ditre forcement erreur.
				}
			}
			predicats = StringUtils.join(andList, Constants._AND_);
			andList.clear();
		}
		return predicats;
	}

	/**
	 * Rajoute le predicat between pour les element numeric ou date
	 * @param not
	 * @param values
	 * @param andList
	 * @param ltValue
	 * @param gtValue
	 * @param fieldName
	 * @param t 
	 */
	private void addBetweenPredicat(final boolean not, final Map<String, Object> values, final List<String> andList, final Object ltValue, final Object gtValue, final String fieldName, final String t) {
		if (!ltValue.equals(gtValue)) {
			int index = values.size();
			values.put("value_" + index, ltValue);
			values.put("value_" + (index + 1), gtValue);
			andList.add((not ? "not " : " ") + t + "." + fieldName + Constants._BETWEEN_+":value_" + index + Constants._AND_+":value_" + (index + 1));
		}

	}

	/**
	 * Genere un ensemble de predicats de type IN séparés par des 'and', et met a jour la hashmap
	 *
	 * @param not : la requete est elle negative.
	 * @param values : Map qui contiendra les valeurs des objets constituant les prédicat
	 * @param t : label de l'objet de la requete
	 * @param tClass : classe de l'objet de la requete
	 * @param entities : liste d'entities possedants les valeurs qui constituront la requete
	 * @return predicats
	 */
	private String getInPredicatsAndValues(final boolean not, final Map<String, Object> values, final String t, final Class tClass, final Object... entities) {
		if (entities == null || entities.length == 0) {
			return "";
		}
		List<String> orList = new ArrayList<String>();
		List<String> andList = new ArrayList<String>();
		for (Method getter : tClass.getMethods()) {
			try {
				Field field = EntityTools.getFieldFromGetter(tClass, getter);
				if (field != null && !field.isAnnotationPresent(Transient.class)) {
					for (Object entity : entities) {
						try {
							Object obj = getter.invoke(entity);
							if (checkValue(obj)) {
								if (obj.getClass().isAnnotationPresent(Entity.class)) {
									// la valeur est une entité
									try {
										// si la valeur ne correspond qu'a une seule entité, on s'en sert comme argument
										obj = _findUniqueLike(obj);
										int index = values.size();
										values.put("entity_" + index, obj);
										orList.add(t + "." + field.getName() + " = :entity_" + index);
									} catch (NoResultException ex) {
										// sinon, on fait la jointure avec ses fields
										orList.add(getInPredicatsAndValues(not, values, t + "." + field.getName(), obj.getClass(), obj));
									} catch (NonUniqueResultException ex) {
										// sinon, on fait la jointure avec ses fields
										orList.add(getInPredicatsAndValues(not, values, t + "." + field.getName(), obj.getClass(), obj));
									}
								} else if (obj instanceof Collection) {
									// Ne marche que si les objets de la collections sont primitifs ou des entitées avec des ids settés
									List<String> memberOf = new ArrayList<String>();
									for (Object o : (Collection) obj) {
										int index = values.size();
										values.put("collection_" + index, o);
										String partSQL;
										partSQL = ":collection_" + index + " MEMBER OF " + t + "." + field.getName();
										memberOf.add(partSQL);
									}
									if (memberOf.size() > 0) {
										String memberOfPredicate = StringUtils.join(memberOf, Constants._AND_);
										orList.add("(" + memberOfPredicate + ")");
									}
								} else if (obj instanceof String) {
									int index = values.size();
									values.put("string_" + index, obj);
									if (((String) (obj)).matches(".*%+.*")) {
										orList.add(t + "." + field.getName() + " LIKE :string_" + index);
									} else {
										orList.add(t + "." + field.getName() + " = :string_" + index);
									}
								} else if (obj.getClass().isEnum()) {
									int index = values.size();
									values.put("enum_" + index, obj);
									orList.add(t + "." + field.getName() + " = :enum_" + index);
								} else if (obj instanceof Float) {
									int index = values.size();
									values.put("float_" + index, obj);
									orList.add(t + "." + field.getName() + " = :float_" + index);
								} else if (obj instanceof Integer) {
									int index = values.size();
									values.put("integer_" + index, obj);
									orList.add(t + "." + field.getName() + " = :integer_" + index);
								} else if (obj instanceof Long) {
									int index = values.size();
									values.put("long_" + index, obj);
									orList.add(t + "." + field.getName() + " = :long_" + index);
								} else if (obj instanceof Short) {
									int index = values.size();
									values.put("short_" + index, obj);
									orList.add(t + "." + field.getName() + " = :short_" + index);
								} else {
									int index = values.size();
									values.put("unknown_" + index, obj);
									orList.add(t + "." + field.getName() + " = :unknown_" + index);
								}
							}
						} catch (IllegalAccessException ex) {
//							log.error("IllegalAccessException : " + tClass.getSimpleName() + "." + getter.getName(), ex);
						} catch (IllegalArgumentException ex) {
//							log.error("IllegalArgumentException : " + tClass.getSimpleName() + "." + getter.getName(), ex);
						} catch (InvocationTargetException ex) {
//							log.error("InvocationTargetException : " + tClass.getSimpleName() + "." + getter.getName(), ex);
						} catch (NoSuchMethodError ex) {
//							log.error("NoSuchMethodError : " + tClass.getSimpleName() + "." + getter.getName(), ex);
						}
					} // add or entre les predicats
					// add or entre les predicats
					String predicat = StringUtils.join(orList, " or ");
					if (!"".equals(predicat)) {
						andList.add((not ? "not " : "") + "(" + predicat + ")");
					}
					orList.clear();
				}
			} // add and entre les predicats
			catch (FieldNotFound ex) {
				// Ne pas logger cette exception, c'est normal.
				// Qui dit exception, ne veut pas ditre forcement erreur.
			}
		} // add and entre les predicats
		String predicats = StringUtils.join(andList, Constants._AND_);
		andList.clear();
		return predicats;
	}

	/**
	 * Retourne si la valeur passé en argument est une valeur correcte pour faire partie de la requete
	 *
	 * @param object
	 * @return
	 */
	private boolean checkValue(final Object object) {
		if (object == null) {
			return false;
		}
		if (object instanceof Boolean) {
			return false;
		}
		if (object instanceof Number) {
			if (object instanceof Float) {
				float i = (Float) object;
				if (i == 0) {
					return false;
				}
			}
			if (object instanceof Integer) {
				int i = (Integer) object;
				if (i == 0) {
					return false;
				}
			}
			if (object instanceof Long) {
				long i = (Long) object;
				if (i == 0) {
					return false;
				}
			}
			if (object instanceof Short) {
				short i = (Short) object;
				if (i == 0) {
					return false;
				}
			}
		}
		if (object instanceof Collection) {
			return true;
		}
		return !object.getClass().isArray();
	}

	/**
	 * Retourne si les valeurs passées en argument sont des valeurs correctes pour faire partie d'une clause between
	 *
	 * @param object
	 */
	private void checkBetweenValue(final Object o1, final Object o2) throws BooleanValueException, NumberValueException, DateValueException, NullValueException {
		if(o1 == o2) { // cas vrai que pour les primitif ou null
			return;
		}
		if (o1 == null || o2 == null) { // un des deux ext nul, on veux tester la nullité de la relation
			throw new NullPointerException();
		}
		if(o1.equals(o2)) { // si les deux éléments ont la même value on veut pas la tester
			return;
		}
		if (o1 instanceof Boolean || o2 instanceof Boolean) {
			throw new BooleanValueException();
		}
		if (o1 instanceof Number || o2 instanceof Number) {
			throw new NumberValueException();
		}
		if (o1 instanceof Date || o2 instanceof Date) {
			throw new DateValueException();
		}
	}

	private void setValuesToQuery(final Query query, final Map<String, Object> values) {
		for (String key : values.keySet()) {
			query.setParameter(key, values.get(key));
		}
	}

	private enum TypeStatement {

		COUNT, SELECT, DELETE
	}

	public abstract boolean exists(final Object id);
}
