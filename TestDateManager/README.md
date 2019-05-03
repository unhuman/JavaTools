# Faking Out Date With Mockito

By Mocking out Date properly, you can improve the speed of your timing dependent Unit Tests.

You can simplify difficult to test timezone scenarios by directly modifying the Date.  This might require additional changes to the TestDateManager class

## Sample Use Case

Some times tests that require delays can take a long time to run.

An example of speeding up delays is a touch() method.  If you wanted your test to have an item that timed out in 2 seconds, a test that touch() worked properly would do the following:

1.  Create the object with a TTL of 2 seconds
- Thread.sleep() for 2 seconds
- Ensure object timed out
- Create the object with a TTL of 2 seconds
- Thread.sleep() for 1 seconds
- perform touch()
- Thread.sleep() for 1 seconds
- Ensure object still available

In the above example we're sleeping for 4 seconds.  This can go down to nanoseconds if we just spoofed the clock.

## Setup

#### pom.xml
```
<properties>
    <!-- Testing library versions -->
    <powermock.version>2.0.0</powermock.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.powermock</groupId>
        <artifactId>powermock-module-junit4</artifactId>
        <version>${powermock.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.powermock</groupId>
        <artifactId>powermock-api-mockito2</artifactId>
        <version>${powermock.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.powermock</groupId>
        <artifactId>powermock-core</artifactId>
        <version>${powermock.version}</version>
        <scope>test</scope>
        <!-- mockito-core has older versions, so we should fix this -->
        <exclusions>
            <exclusion>
                <groupId>net.bytebuddy</groupId>
                <artifactId>byte-buddy</artifactId>
            </exclusion>
            <exclusion>
                <groupId>net.bytebuddy</groupId>
                <artifactId>byte-buddy-agent</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
</dependencies>
```

#### Test Class Initialization
```
@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.yourCompany.yourNamespace.*")
public class YourTest {
   .......
```

#### TestDateManager Initialization
```
@Before
public void setup() {
    MockitoAnnotations.initMocks(this);

    // every time we call new Date() inside a method of any class
    // declared in @PrepareForTest we will get the FakeData instance
    testDateManager = new TestDateManager();

    // new Date() - we want to get a copy of the current testDateManager Date (since it can change)
    PowerMockito.whenNew(Date.class).withNoArguments().thenAnswer(answer ->
        TestDateManager.createDate(testDateManager.getTime())
    );
    // new Date(x) - respect the time specified
    PowerMockito.whenNew(Date.class).withArguments(anyLong()).thenAnswer(answer -> {
        long time = answer.getArgument(0);
        return TestDateManager.createDate(time);
    });
}

```
## Usage

Now, whenever your application calls new Date(), Mockito will leverage the below TestDateManager class to create dates artificially.

Instead of using Thread.sleep() in your test code, use testDateManager.advanceTimeMilliseconds().

Whenever obtaining time within your code, you should use new Date().getTime() and not System.currentTimeMillis().

Alternatively, one could Mock System.currentTimeMillis() similarly, as Java Date uses that under the covers.
