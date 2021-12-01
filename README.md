# B+Tree

An implementation of B+Tree  (a multiway search tree based on the memory, i.e., all data records are stored in the memory instead of the disk) . 



![](https://img.shields.io/badge/license-Apache%202.0-green)	![](https://img.shields.io/badge/coverage-100%25-brightgreen)	![](https://img.shields.io/badge/maven%20central-v1.2.0-blue)	 ![](https://img.shields.io/badge/java%20version-8-red)



## Getting started

Add dependency (maven) :

```xml
<dependency>
  <groupId>xyz.proadap.aliang</groupId>
  <artifactId>MemoryBasedBPlusTree</artifactId>
  <version>1.2.0</version>
</dependency>
```



Instantiation of BPlusTree

```java
/**
* BPlusTree<K,E>
* K is the genericity of entry that need to be comparable
* E is the genericity of data item that can be any object
*/
BPlusTree<Integer,String> bPlusTree = new BPlusTree<>(16);//instance B+Tree with specific order

//instance B+Tree with default order
BPlusTree<Integer,String> bPlusTree = new BPlusTree<>(); //default order is 9
```



## Features

#### insert

```java
bPlusTree.insert(0, "data record 1");
bPlusTree.insert(0, "data record 2");
bPlusTree.insert(1, "data record 3");
bPlusTree.insert(2, "data record 4");
bPlusTree.insert(3, "data record 5");
```



#### query

```java
//query all data records under the entry 0
List<String> queryResult = bPlusTree.query(0);
System.out.println(queryResult); //[data record 2, data record 1]
```



#### range query

```java
//query all data records under the entries [0,3)
List<String> queryResult = bPlusTree.rangeQuery(0, 3);
System.out.println(queryResult); //[data record 2, data record 1, data record 3, data record 4]
```



#### update

```java
//return true if update successfully, false otherwise
bPlusTree.update(0, "data record 2", "data record 12");
System.out.println(bPlusTree.query(0)); //[data record 1, data record 12]
```



#### remove

```java
/**
* remove all data records under the entry
* return true if remove successfully, false otherwise
*/
bPlusTree.remove(1);
System.out.println(bPlusTree.query(1)); //[]

/**
* remove specific data record under the entry (using ".equals()" to determine whether certain data record is the present)
* return true if remove successfully, false otherwise
*/
bPlusTree.remove(0, "data record 12");
System.out.println(bPlusTree.query(0)); //[data record 1]
```



### License

BPlusTree is under the Apache 2.0 license. See the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0) file for details.

------

If it is helpful for you, you could give me a star to support me.

