/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.hhdev.xcrud;

import fr.hhdev.xcrud.exceptions.EntityIdNotFound;
import fr.hhdev.xcrud.exceptions.FieldNotFound;
import fr.hhdev.xcrud.exceptions.MethodNotFound;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Administrateur
 */
public class EntityTools {

	/**
	 * Cherche la methode pour acceder à l'id de l'entity. Recherche dans la hierarchie aussi.
	 *
	 * @param entity
	 * @return
	 * @throws fr.hhdev.xcrud.exceptions.EntityIdNotFound
	 * @throws fr.hhdev.xcrud.exceptions.MethodNotFound
	 */
	public static Method getIdentityGetter(Object entity) throws EntityIdNotFound, MethodNotFound {
		Class tClass = entity.getClass();
		//  on essaye de determiné si on trouve le field Annoté @id
		while (tClass.isAnnotationPresent(Entity.class)) {
			Field[] fields = tClass.getDeclaredFields();
			for (Field field : fields) {
				if (field.isAnnotationPresent(Id.class)) {
					return getGetterFromField(tClass, field);
				}
			}
			tClass = tClass.getSuperclass();
		}
		throw new EntityIdNotFound();
	}

	/**
	 * retourne la méthode getter associée à un field pour une classe donnée
	 *
	 * @param tClass
	 * @param field
	 * @return Method
	 * @throws fr.hhdev.xcrud.exceptions.MethodNotFound
	 */
	public static Method getGetterFromField(Class tClass, Field field) throws MethodNotFound {
		String fieldName = field.getName();
		String getterName = StringUtils.capitalize(fieldName);
		if (field.getType().equals(Boolean.class)) {
			getterName = Constants.IS + getterName;
		} else {
			getterName = Constants.GET + getterName;
		}
		try {
			return tClass.getMethod(getterName);
		} catch (NoSuchMethodException ex) {
			throw new MethodNotFound();
		} catch (SecurityException ex) {
			throw new MethodNotFound();
		}
	}

	/**
	 * retourne le field pour une method de type getter ignore la methode getClass
	 *
	 * @param tClass
	 * @param getter
	 * @return
	 * @throws fr.hhdev.xcrud.exceptions.FieldNotFound
	 */
	public static Field getFieldFromGetter(Class tClass, Method getter) throws FieldNotFound {
		if (getter.isAnnotationPresent(Lob.class)) {
			throw new FieldNotFound();
		}
		if (getter.getReturnType() == null) {
			throw new FieldNotFound();
		}
		if (Modifier.isStatic(getter.getModifiers())) {
			throw new FieldNotFound();
		}
		if (getter.getName().equals(Constants.GETCLASS)) {
			throw new FieldNotFound();
		}
		String fieldName = getFieldName(getter.getName());
		return getField(tClass, fieldName);
	}

	/**
	 * A partir du nom du fieldname, retorune le field
	 * @param cl
	 * @param fieldName
	 * @return
	 * @throws FieldNotFound 
	 */
	private static Field getField(final Class cl, final String fieldName) throws FieldNotFound {
		Class tClass = cl;
		boolean found = false;
		while (!found && (tClass.isAnnotationPresent(Entity.class) || tClass.isAnnotationPresent(MappedSuperclass.class))) {
			try {
				Field field = tClass.getDeclaredField(fieldName);
				if (field.isAnnotationPresent(Lob.class)) {
					throw new FieldNotFound();
				}
				return field;
			} catch (NoSuchFieldException ex) { // on verifie dans la hierarchie aussi
				tClass = tClass.getSuperclass();
			}
		}
		throw new FieldNotFound();
	}
	
	/**
	 * Retourne le nom du field que doit donner accès le getter
	 * @param getterName
	 * @return
	 * @throws FieldNotFound 
	 */
	private static String getFieldName(String getterName) throws FieldNotFound {
		String fieldName = null;
		if (getterName.startsWith(Constants.GET)) {
			fieldName = getterName.substring(3);
		} else if (getterName.startsWith(Constants.IS)) {
			fieldName = getterName.substring(2);
		} else throw new FieldNotFound();
		// on verifie que ce getter correspond bien à un field
		return StringUtils.uncapitalize(fieldName);
	}
}
