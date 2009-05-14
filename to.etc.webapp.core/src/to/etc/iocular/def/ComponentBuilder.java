package to.etc.iocular.def;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import to.etc.iocular.BindingScope;
import to.etc.iocular.container.BuildPlan;
import to.etc.iocular.container.FailedAlternative;
import to.etc.iocular.container.MethodInvoker;
import to.etc.iocular.util.ClassUtil;

/**
 * Thingy which helps with building a component definition.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Mar 27, 2007
 */
public class ComponentBuilder {
	private final BasicContainerBuilder	m_builder;

	private final String			m_definitionLocation;

	private final List<String>		m_nameList = new ArrayList<String>();

	/**
	 * If this class is registered as a "defined" type only that type will be registered in the type table.
	 */
	private final List<Class<?>>	m_definedTypeList = new ArrayList<Class<?>>();

	private Class<?>				m_baseClass;

	private Class<?>				m_factoryClass;

	/** The alternative instances of methods providing the object using the static factoryClass */
	private List<Method>			m_factoryMethodList;

	/** When a factory instance has the method to call this contains the key for the factory instance. */
	private String					m_factoryInstance;

	private String					m_factoryMethodText;

	private BindingScope			m_scope;

	private boolean					m_autowire;

	private String					m_creationString;

	/**
	 * The actual type created by this definition.
	 */
	private Class<?>				m_actualType;

	private MethodCallBuilder		m_currentMethodBuilder;

	private final List<MethodCallBuilder>	m_factoryStartList = new ArrayList<MethodCallBuilder>();

	private final List<MethodCallBuilder>	m_startList = new ArrayList<MethodCallBuilder>();

	ComponentBuilder(final BasicContainerBuilder b, final String loc) {
		m_builder = b;
		m_definitionLocation = loc;
	}
	public String getDefinitionLocation() {
		return m_definitionLocation;
	}
	public BasicContainerBuilder	getBuilder() {
		return m_builder;
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Builder construction methods.						*/
	/*--------------------------------------------------------------*/
	private void checkCreation() {
		if(m_creationString != null)
			throw new IocConfigurationException(this.m_builder, getDefinitionLocation(), "Component already created by "+m_creationString);
	}

	/**
	 * When called this constructs a concrete type from the class passed, using either constructor
	 * or setter injection as needed. This defines a base creation method and so it forbids the
	 * other creation methods.
	 */
	public ComponentBuilder	type(final Class<?> clz) {
		checkCreation();

		//-- Check to see if the class is acceptable
		int mod = clz.getModifiers();
		if(Modifier.isAbstract(mod))
			throw new IocConfigurationException(this, this+": the class "+clz+" is abstract");
		if(! Modifier.isPublic(mod))
			throw new IocConfigurationException(this, this+": the class "+clz+" is not public");
		if(clz.isInterface())
			throw new IocConfigurationException(this, this+": the class "+clz+" is an interface");

		Constructor<?>[]	car = clz.getConstructors();
		if(car == null || car.length == 0)	// Cannot construct
			throw new IocConfigurationException(this, this+": the class "+clz+" has no public constructors");

		//-- Define a type plan, and register it; This is a constructor-defined type...
		m_actualType	= clz;
		m_baseClass		= clz;
		m_creationString = "creating a new class instance";
		m_definedTypeList.add(clz);
		return this;
	}

//	/**
//	 * Defines a static factory class as a component. This merely registers the class as a
//	 * lifetime-controlled thingy; instances of this class are never resolved.
//	 *
//	 * @param clz
//	 * @return
//	 */
//	public ComponentBuilder	staticClass(Class<?> clz) {
//		m_
//
//
//		return this;
//	}

	/**
	 * Define an explicit type for this class;
	 * @param clz
	 * @return
	 */
	public ComponentBuilder	implement(final Class<?> clz) {
		m_definedTypeList.add(clz);
		return this;
	}

	/**
	 * <p>A basic object builder defining an object to be returned from a
	 * static factory method on a class. The method passed must be
	 * resolvable to a static method on the class passed, and it's
	 * parameters must be fillable from the container. After this call
	 * we'll have a "method" current so the calls to set method parameters
	 * work and will define parameters for this method.</p>
	 *
	 * <p>This method defines creation so it throws up if another creation method
	 * is already defined.</p>
	 *
	 * @param clz
	 * @param method
	 * @return
	 */
	public ComponentBuilder	factory(final Class<?> clz, final String method) {
		checkCreation();
		m_factoryMethodList	= findMethodInFactory(clz, method, true);
		m_factoryClass	= clz;
		m_actualType	= m_factoryMethodList.get(0).getReturnType();
		m_creationString = "calling static factory method "+method+" on class "+clz;
		return this;
	}

	private List<Method>	findMethodInFactory(final Class<?> clz, final String method, final boolean mbstatic) {
		Method[] mar = ClassUtil.findMethod(clz, method);
		if(mar.length == 0)
			throw new IocConfigurationException(this, "Method "+method+" is not defined in class '"+clz+"'");
		List<Method>	thelist = new ArrayList<Method>();
		Class<?>		rtype = null;
		for(int i = mar.length; --i >= 0;) {
			Method m = mar[i];

			int mod = m.getModifiers();
			if(mbstatic && ! Modifier.isStatic(mod))
				continue;
			if(! Modifier.isPublic(mod))
				continue;
			Class<?>	c = m.getReturnType();
			if(c == Void.TYPE)
				continue;
			if(c.isPrimitive())
				continue;
			if(rtype == null)
				rtype = c;
			else if(rtype != c)
				throw new IocConfigurationException(this, "The "+mar.length+" different overloads of the method "+method+" return different types");
			thelist.add(m);
		}
		if(rtype == null)
			throw new IocConfigurationException(this, "None of the "+mar.length+" versions of the method "+method+" is usable as a"+(mbstatic ? "static " : "")+" public factory method returning an object");
		return thelist;
	}

	/**
	 * A basic object builder defining an object to be returned from a container object identified
	 * by a name, by calling a method on that object.
	 *
	 * @param id
	 * @param method
	 * @return
	 */
	public ComponentBuilder	factory(final String id, final String method) {
		checkCreation();
		m_factoryInstance	= id;
		m_factoryMethodText	= method;
		m_creationString = "Calling a factory method on object reference "+id;
		return this;
	}

	/**
	 * When called this adds a name for the component. A single component can have more than
	 * one name, but the name must be unique within the container it gets stored in.
	 * @param name
	 */
	public ComponentBuilder name(final String name) {
		m_builder.addComponentName(this, name);			// Register another name with the configuration; throws up when duplicate
		m_nameList.add(name);
		return this;
	}

	/**
	 * Defines the scope for this object. This defaults to "SINGLETON"
	 * @param scope
	 * @return
	 */
	public ComponentBuilder scope(final BindingScope scope) {
		m_scope = scope;
		return this;
	}
	public ComponentBuilder	autowire(final boolean yes) {
		m_autowire = yes;
		return this;
	}
	public ComponentBuilder	destroy(final Class<?> wh, final String what) {
		return this;
	}


	/**
	 * Only used for static factories, this allows you to call a static method on the
	 * container class itself to get it to initialize.
	 *
	 * @param methodName
	 * @param arguments
	 * @return
	 */
	public ComponentBuilder	factoryStart(final String methodName, final Class<?>... arguments) {
		//-- Make sure we're a static factory thingy.
		if(m_factoryClass == null)
			throw new IocConfigurationException(this, "factoryStart() can only be used for static factory classes.");
		MethodCallBuilder	mcb = new MethodCallBuilder(this, m_factoryClass, methodName, arguments, true);
		m_currentMethodBuilder = mcb;
		m_factoryStartList.add(mcb);
		return this;
	}

	/**
	 * Only used for static factories, this allows you to call a static method on whatever
	 * static class to get it to initialize.
	 *
	 * @param methodName
	 * @param arguments
	 * @return
	 */
	public ComponentBuilder	factoryStart(final Class<?> clz, final String methodName, final Class<?>... arguments) {
		//-- Make sure we're a static factory thingy.
		if(m_factoryClass == null)
			throw new IocConfigurationException(this, "factoryStart() can only be used for static factory classes.");
		MethodCallBuilder	mcb = new MethodCallBuilder(this, clz, methodName, arguments, true);
		m_currentMethodBuilder = mcb;
		m_factoryStartList.add(mcb);
		return this;
	}


	/**
	 * Add a start method to an object being retrieved.
	 *
	 * @param methodName
	 * @param arguments
	 * @return
	 */
	public ComponentBuilder	start(final String methodName, final Class<?>... arguments) {
		//-- Make sure we're a static factory thingy.
		if(m_factoryClass == null)
			throw new IocConfigurationException(this, "factoryStart() can only be used for static factory classes.");
		MethodCallBuilder	mcb = new MethodCallBuilder(this, m_factoryClass, methodName, arguments, false);
		m_currentMethodBuilder = mcb;
		m_startList.add(mcb);
		return this;
	}

	List<String>	getNameList() {
		return m_nameList;
	}

	public String	getIdent() {
		if(m_nameList.size() > 0)
			return "component(name="+m_nameList.get(0)+")";
		if(m_definedTypeList.size() > 0)
			return "component(type="+m_definedTypeList.get(0).toString()+")";
		return "component(Unnamed/untyped)";
	}

	@Override
	public String toString() {
		if(m_nameList.size() > 0)
			return "component(name="+m_nameList.get(0)+") defined at "+m_definitionLocation;
		if(m_definedTypeList.size() > 0)
			return "component(type="+m_definedTypeList.get(0).toString()+") defined at "+m_definitionLocation;
		return "component(Unnamed/untyped) defined at "+m_definitionLocation;
	}

	private ParameterDef[]	getParameterDef() {
		return null;			// TODO Make this do something.
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Type registration.									*/
	/*--------------------------------------------------------------*/
	/**
	 * Called early when the build is complete, this registers all of the
	 * types provided by this component. For "easy" objects like class and
	 * static factories this merely defines the created type and all
	 * implemented interfaces and such. For factories that use some kind
	 * of container reference with a method this determines the type of
	 * object returned by decoding the reference and the method.
	 */
	void registerTypes() {
		if(m_definedTypeList.size() > 0) {
			//-- Register as "




		}







	}



	/**
	 * Calculates the actual type for this definition. This can get
	 * called recursively when ref factories use other ref factories, in which
	 * case a circular ref can occur. We test for this using the call stack.
	 *
	 * @param stack
	 * @return
	 */
	Class<?> calculateType(final Stack<ComponentBuilder> stack) {
		if(m_actualType != null)
			return m_actualType;

		//-- Type as-of-yet undefined. Handle late-typing possibilities
		if(m_factoryInstance != null) {
			if(m_factoryMethodText == null)
				throw new IocConfigurationException(this, "Missing method specification on a 'ref' factory");

			//-- We must be able to at least get a 'type' for the ID, from whatever container.
			if(stack.contains(this)) {
				//-- Recursive definition....
				throw new IocConfigurationException(this, "Circular reference to "+this);
			}
			stack.push(this);
			Class<?>	clz = m_builder.calcTypeByName(stack, m_factoryInstance);
			if(this != stack.pop())
				throw new IllegalStateException("Stack inbalance!?");
			if(clz == null)
				throw new IocConfigurationException(this, "The component with id='"+m_factoryInstance+"' is not known");

			//-- The 'factory' type is known; now retrieve the method;
			m_factoryMethodList	= findMethodInFactory(clz, m_factoryMethodText, false);
			m_actualType	= m_factoryMethodList.get(0).getReturnType();
			return m_actualType;
		} else
			throw new IocConfigurationException(this, "Can't determine the 'type' of this component: no factory defined.");
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Builder get info methods.							*/
	/*--------------------------------------------------------------*/
	private ComponentRef		m_ref;

	ComponentRef	calculateComponentRef(final Stack<ComponentBuilder> stack) {
		if(m_ref != null)
			return m_ref;

		//-- We need to build this. Are we not already building (circular reference)?
		if(stack.contains(this))
			throw new IocCircularException(this, stack, "Circular reference");
		stack.push(this);
		BuildPlan	plan = createBuildPlan(stack);
		if(this != stack.pop())
			throw new IllegalStateException("Stack mismatch!?");

		//-- Create the def for this object
		ComponentDef	def = new ComponentDef(
				getActualClass(),
				getNameList().toArray(new String[getNameList().size()]),
				getDefinedTypes().toArray(new Class<?>[getDefinedTypes().size()]),
				getScope(),
				getDefinitionLocation(),
				plan
				);
		m_ref	= new ComponentRef(def, getBuilder().getContainerIndex());

		return m_ref;
	}

	private BuildPlan	createBuildPlan(final Stack<ComponentBuilder> stack) {
		BuildPlan	bp = createCreationBuildPlan(stack);		// Create the actual object,

		//-- Append all other crud to the build plan: start with start- and destroy- code



		return bp;
	}

	/**
	 * @return
	 */
	private BuildPlan	createCreationBuildPlan(final Stack<ComponentBuilder> stack) {
		//-- 1. Normal class instance plan?
		if(m_baseClass != null)
			return createConstructorPlan(stack);

		if(m_factoryMethodList != null)
			return createStaticFactoryBuildPlan(stack);

		throw new IocConfigurationException(this, "Cannot create a build plan!?");
	}




	/*--------------------------------------------------------------*/
	/*	CODING:	Constructor-based build plan						*/
	/*--------------------------------------------------------------*/

	private BuildPlan	createConstructorPlan(final Stack<ComponentBuilder> stack) {
		Constructor<?>[]	car = m_baseClass.getConstructors();
		if(car == null || car.length == 0)	// Cannot construct
			throw new IocConfigurationException(this, "The class "+m_baseClass+" has no public constructors");
		List<ConstructorBuildPlan>	list = new ArrayList<ConstructorBuildPlan>();
		List<FailedAlternative>	aflist = new ArrayList<FailedAlternative>();
		for(Constructor<?> c : car) {
			if(! Modifier.isPublic(c.getModifiers())) {
				aflist.add(new FailedAlternative("The constructor "+c+" is not public"));
				continue;
			}
			ConstructorBuildPlan	cbp = calcConstructorPlan(stack, c, aflist);
			if(cbp != null)
				list.add(cbp);
		}
		if(list.size() == 0)
			throw new BuildPlanFailedException(this, "None of the constructors was usable", aflist);

		//-- Find the plan with the highest score.
		ConstructorBuildPlan best = list.get(0);
		for(int i = 1; i < list.size(); i++) {
			ConstructorBuildPlan bp = list.get(i);
			if(bp.getScore() > best.getScore())
				best = bp;
		}
		return best;
	}

	/**
	 * Try to create a build plan for creating the object using the constructor passed.
	 *
	 * @param cont
	 * @param stack
	 * @param c
	 * @param aflist
	 * @return
	 */
	private ConstructorBuildPlan	calcConstructorPlan(final Stack<ComponentBuilder> stack, final Constructor<?> c, final List<FailedAlternative> aflist) {
		Class<?>[]	fpar = c.getParameterTypes();		// Formals.
		Annotation[][]	fpanar = c.getParameterAnnotations();
		if(fpar == null || fpar.length == 0) {
			return new ConstructorBuildPlan(c, 0);		// Always works but with score=0
		}

		//-- Walk all parameters and make build plans for them until failure..
		try {
			ParameterDef[]	paref	= getParameterDef();
			List<ComponentRef>	actuals = calculateParameters(stack, fpar, fpanar, paref);

			//-- All constructor arguments were provided- return a build plan,
			return new ConstructorBuildPlan(c, fpar.length, actuals.toArray(new ComponentRef[actuals.size()]));
		} catch(IocUnresolvedParameterException x) {
			//-- This constructor has failed.
			FailedAlternative	fa = new FailedAlternative("The constructor "+c+" is unusable: "+x.getMessage());
			aflist.add(fa);
			return null;
		}
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Factory-based build plan.							*/
	/*--------------------------------------------------------------*/

	private BuildPlan	createStaticFactoryBuildPlan(final Stack<ComponentBuilder> stack) {
		//-- Walk all factoryStart methods that are defined and create the methodlist from them
		List<MethodInvoker>	startlist = createCallList(stack, m_factoryStartList);

		//-- Walk all possible factory methods, scoring them,
		List<StaticFactoryBuildPlan>	list = new ArrayList<StaticFactoryBuildPlan>();
		List<FailedAlternative>	aflist = new ArrayList<FailedAlternative>();
		for(Method m : m_factoryMethodList) {
			StaticFactoryBuildPlan	cbp = calcStaticFactoryPlan(stack, m, aflist, startlist);
			if(cbp != null)
				list.add(cbp);
		}
		if(list.size() == 0)
			throw new BuildPlanFailedException(this, "None of the factory methods was usable", aflist);

		//-- Find the plan with the highest score.
		StaticFactoryBuildPlan best = list.get(0);
		for(int i = 1; i < list.size(); i++) {
			StaticFactoryBuildPlan bp = list.get(i);
			if(bp.getScore() > best.getScore())
				best = bp;
		}
		return best;
	}

	private StaticFactoryBuildPlan	calcStaticFactoryPlan(final Stack<ComponentBuilder> stack, final Method c, final List<FailedAlternative> aflist, final List<MethodInvoker> startlist) {
		Class<?>[]	fpar = c.getParameterTypes();		// Formals.
		Annotation[][]	fpanar = c.getParameterAnnotations();
		if(fpar == null || fpar.length == 0) {
			return new StaticFactoryBuildPlan(c, 0, BuildPlan.EMPTY_PLANS, startlist);	// Always works but with score=0
		}

		//-- Walk all parameters and make build plans for them until failure..
		try {
			ParameterDef[]	paref	= getParameterDef();
			List<ComponentRef>	actuals = calculateParameters(stack, fpar, fpanar, paref);

			//-- All constructor arguments were provided- return a build plan,
			return new StaticFactoryBuildPlan(c, fpar.length, actuals.toArray(new ComponentRef[actuals.size()]), startlist);
		} catch(IocUnresolvedParameterException x) {
			//-- This constructor has failed.
			FailedAlternative	fa = new FailedAlternative("The static factory method "+c+" is unusable: "+x.getMessage());
			aflist.add(fa);
			return null;
		}
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Call invoker build plan calculator.					*/
	/*--------------------------------------------------------------*/

	private List<MethodInvoker>	createCallList(final Stack<ComponentBuilder> stack, final List<MethodCallBuilder> list) {
		List<MethodInvoker>	res = new ArrayList<MethodInvoker>(list.size());
		for(MethodCallBuilder mcb : list)
			res.add(mcb.createInvoker(stack));
		return res;
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Parameter calculators.								*/
	/*--------------------------------------------------------------*/

	private List<ComponentRef>	calculateParameters(final Stack<ComponentBuilder> stack, final Class<?>[] fpar, final Annotation[][] fpann, final ParameterDef[] defar) {
		List<ComponentRef>	actuals = new ArrayList<ComponentRef>();
		for(int i = 0; i < fpar.length; i++) {
			Class<?> fp = fpar[i];
			ParameterDef	def = null;
			if(defar != null && i < defar.length)
				def = defar[i];
			ComponentRef	cr	= m_builder.findReferenceFor(stack, fp, fpann[i], def);
			if(cr == null) {
				//-- Cannot use this- the parameter passed cannot be filled in.
				throw new IocUnresolvedParameterException("Parameter "+i+" (a "+fp+") cannot be resolved");
			}
			actuals.add(cr);
		}
		return actuals;
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Accessors.											*/
	/*--------------------------------------------------------------*/

	Class<?>		getActualClass() {
		if(m_actualType == null)
			throw new IllegalStateException("calculateType has not yet been called");
		return m_actualType;
	}

	List<Class<?>>	getDefinedTypes() {
		return m_definedTypeList;
	}
	public BindingScope getScope() {
		return m_scope;
	}

}