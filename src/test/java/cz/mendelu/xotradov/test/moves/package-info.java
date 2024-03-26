package cz.mendelu.xotradov.test.moves;
/**
 * There is an concurency error somewhere. When the tests were run as pasrt of single class,
 * sometimes some leftover was not garbagecollected in time, and thus one or two tests randomly failed
 * on cleanup, because the *rule* could not clean between runs.
 *
 * To have each of them in own file, with own RULE is much slower, but the concurency issue moreover disapeared
 * Still, may appear from time to time:(
 */
